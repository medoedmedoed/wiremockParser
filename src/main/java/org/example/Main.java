package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
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
    public static final String HOSTINFLUX = "10.230.33.176:8086"; //"localhost:8086";  //"10.230.33.176:8086";


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Start application");

        File folder = new File("C:\\wiremock");
        if(args.length > 0) {
            folder = new File(args[0]);
        }
        List<ForInflux> finalList = new ArrayList<>();
        File[] folderInWiremock = folder.listFiles();                               //получаем все папки в wiremock

        for (int i = 0; i < folderInWiremock.length; i++) {
            System.out.println(folderInWiremock[i]);
        }

        System.out.println("Mocks:");

        for (int i = 0, k = 0; i < folderInWiremock.length; i++) {

            File folderMappings = new File(folderInWiremock[i] + "/mappings");
            ArrayList<File> filesIntoFolderMappings = new ArrayList<File>(Arrays.asList(folderMappings.listFiles())); //получаем jsonы внутри i-той папки c:\wiremock

            for (int j = 0; j < filesIntoFolderMappings.size(); j++, k++) {
                try{
                    finalList.add(ParseFile.parseFile(filesIntoFolderMappings.get(j))); //парсим каждый файл. Достаем метод, url и порт
                    finalList.get(k).setPort(ParseFile.parseBatFile(folderInWiremock[i]));
                    System.out.println(finalList.get(k).toString());
                }catch (FileNotFoundException e){
                    System.out.println("Не найден файл start.bat в " + folderInWiremock[i].getName() + " для " + filesIntoFolderMappings.get(j));
                }
            }

        }

        Thread.sleep(10000);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        //-------------------------------------------------------------------------------------------------

        HttpRequest requestToMock;
        HttpRequest requestToInflux;

        HttpRequest.BodyPublisher bodyPublisherToInflux;

        HttpResponse<String> responseToMock;
        HttpResponse<String> responseToInflux;

        //отправляем один раз инфу по урлам и портам

        ArrayList<ForInflux> listFolderToCheckRequest = new ArrayList<>();

        for(int i = 0; finalList.size() > i; i++){

            if(finalList.get(i).getUrlPattern().equals("/check")){
                listFolderToCheckRequest.add(finalList.get(i));
            }

            bodyPublisherToInflux = HttpRequest.BodyPublishers.ofString("wiremock," + finalList.get(i).toString());

            requestToInflux = HttpRequest
                    .newBuilder()
                    .POST(bodyPublisherToInflux)
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .uri(URI.create("http://" + HOSTINFLUX + "/write?db=wiremock"))
                    .build();

            responseToInflux = client.send(requestToInflux, HttpResponse.BodyHandlers.ofString());
        }

        while (true){
            for(int i = 0; listFolderToCheckRequest.size() > i; ++i){
                try {
                    requestToMock = HttpRequest
                            .newBuilder()
                            .GET()
                            .uri(URI.create("http://" + HOSTMOCKS + ":" + listFolderToCheckRequest.get(i).getPort() + listFolderToCheckRequest.get(i).getUrlPattern()))
                            .build();

                    responseToMock = client.send(requestToMock, HttpResponse.BodyHandlers.ofString());

                    listFolderToCheckRequest.get(i).setCount(200);

                    System.out.println(responseToMock.toString());

                }catch (HttpConnectTimeoutException | SocketException e){
                    System.out.println("Error to connect into " + listFolderToCheckRequest.get(i).getNameFolder());
                    listFolderToCheckRequest.get(i).setCount(0);
                }finally {
                    //отправляем статус в инфлюкс
                    bodyPublisherToInflux = HttpRequest.BodyPublishers.ofString("wiremock," + listFolderToCheckRequest.get(i).toString());

                    requestToInflux = HttpRequest
                            .newBuilder()
                            .POST(bodyPublisherToInflux)
                            .header("Content-Type", "text/plain; charset=utf-8")
                            .uri(URI.create("http://" + HOSTINFLUX + "/write?db=wiremock"))
                            .build();

                    responseToInflux = client.send(requestToInflux, HttpResponse.BodyHandlers.ofString());
                }
            }

            System.out.println("--------------------------------------------------------------------------------------------");
            Thread.sleep(600000);
        }



        //великолепный код, который отлавливает количество обращений в каждую заглушку
        /*while(true) {
            for (int i = 0; i < finalList.size(); i++) {   //ходим опрашивать по всем активным заглушкам
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

        }*/
    }
}