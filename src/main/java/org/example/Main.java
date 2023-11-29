package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static final String HOSTMOCKS = "localhost";
    public static final String HOSTINFLUX = "10.230.33.176:8086";


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Start application");

        File folder = new File(args[0]);
        List<ForInflux> finalList = new ArrayList<>();

        File[] folderInWiremock = folder.listFiles();                               //получаем все папки в wiremock

        for (int i = 0; i < folderInWiremock.length; i++) {
            System.out.println(folderInWiremock[i]);
        }

        System.out.println("Mocks:");

        for (int i = 0, k = 0; i < folderInWiremock.length; i++) {

            File folderMappings = new File(folderInWiremock[i] + "/mappings");
            ArrayList<File> filesIntoFolderMappings = new ArrayList<File>(Arrays.asList(folderMappings.listFiles()));

            for (int j = 0; j < filesIntoFolderMappings.size(); j++, k++) {
                finalList.add(ParseFile.parseFile(filesIntoFolderMappings.get(j)));
                finalList.get(k).setPort(ParseFile.parseBatFile(folderInWiremock[i]));
                System.out.println(finalList.get(k).toString());
            }

        }

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        //-------------------------------------------------------------------------------------------------
        HttpRequest request;
        HttpRequest request1;
        HttpRequest request2;

        HttpRequest.BodyPublisher bodyPublisher;
        HttpRequest.BodyPublisher bodyPublisherToInflux;

        HttpResponse<String> response;
        HttpResponse<String> response1;
        HttpResponse<String> response2;


        while(true) {
            for (int i = 0; i < finalList.size(); i++) {
                String bodyToMock = "{\"method\": \"" + finalList.get(i).getMethod() + "\"," +
                        "\"url\": \"" + finalList.get(i).getUrlPattern() + "\"}";

                try {
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(bodyToMock);

                    request = HttpRequest
                            .newBuilder()
                            .POST(bodyPublisher)
                            .header("Content-Type", "application/json")
                            .uri(URI.create("http://" + HOSTMOCKS + ":" + finalList.get(i).getPort() + "/__admin/requests/count"))
                            .build();

                    response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    Pattern patternResponse = Pattern.compile("[0-9]+");
                    Matcher matcherResponse = patternResponse.matcher(response.body().toString());
                    matcherResponse.find();
                    finalList.get(i).setCount(Integer.parseInt(matcherResponse.group()));


                    //отправка в influx
                    String bodyToInflux = "wiremock," + finalList.get(i).toString();
                    bodyPublisherToInflux = HttpRequest.BodyPublishers.ofString(bodyToInflux);

                    request2 = HttpRequest
                            .newBuilder()
                            .POST(bodyPublisherToInflux)
                            .header("Content-Type", "text/plain; charset=utf-8")
                            .uri(URI.create("http://" + HOSTINFLUX + "/write?db=wiremock"))
                            .build();

                    response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

                }catch (HttpConnectTimeoutException | SocketException e){
                    System.out.println("Error to connect into " + finalList.get(i).getNameFolder());
                    finalList.get(i).setCount(-1);
                    continue;
                }

            }

            Iterator<ForInflux> iterator = finalList.iterator();
            while(iterator.hasNext()){
                if(iterator.next().getCount()==-1){
                    iterator.remove();
                }
            }

            if(finalList.isEmpty()) {
                System.out.println("no mocks running");
                break;
            }

            for (int i = 0; i < finalList.size(); i++) {
                //очистка журнала
                request1 = HttpRequest
                        .newBuilder()
                        .DELETE()
                        .header("Content-Type", "application/json")
                        .uri(URI.create("http://" + HOSTMOCKS + ":" + finalList.get(i).getPort() + "/__admin/requests"))
                        .build();

                response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
            }

            System.out.println(new SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(new Date().getTime()));
            for (int i = 0; i < finalList.size(); i++) {
                System.out.println(finalList.get(i).toString());
            }
            System.out.println("--------------------------------------------------------------------------------------------");
            Thread.sleep(60000);

        }
    }
}