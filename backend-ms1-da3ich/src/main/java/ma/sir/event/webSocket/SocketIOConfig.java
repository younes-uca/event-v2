package ma.sir.event.webSocket;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import io.socket.emitter.Emitter;
import lombok.extern.log4j.Log4j2;
import ma.sir.event.bean.core.Evenement;
import ma.sir.event.bean.core.EvenementRedis;
import ma.sir.event.dao.facade.core.EvenementDao;
import ma.sir.event.service.impl.admin.EvenementAdminRedisServiceImpl;
import ma.sir.event.ws.converter.EvenementRedisConverter;
import ma.sir.event.zynerator.security.bean.User;
import org.apache.poi.ss.formula.functions.Even;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import reactor.core.publisher.Flux;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import io.socket.client.Socket;

@CrossOrigin("*")
@Component
@Log4j2
@org.springframework.context.annotation.Configuration
public class SocketIOConfig {
    private SocketIOServer server;
    public Map<String, List<String>> sessions = new ConcurrentHashMap<>();

    @Autowired
    private EvenementDao evenementDao;
    @Autowired
    private EvenementAdminRedisServiceImpl evenementAdminRedisService;
    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(8088);
        config.setMaxHttpContentLength(5 * 1024 * 1024); // 5MB
        config.setPingTimeout(60000); // set the ping timeout to 1min
        config.setPingInterval(25000); // set the ping interval to 25s
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(60000); // set the upgrade timeout to 1h
        server = new SocketIOServer(config);
//        server.addEventListener("message", String.class, onSendMessage);
        server.addEventListener("userConnect", User.class, onConnect);
        server.addEventListener("userDisconnect", User.class, onDisConnect);
//        server.addEventListener("search_objects", String.class, (client, ref, ackSender) -> {
//            // Perform search logic to retrieve objects with the same reference
//            List<Evenement> matchedObjects = searchObjectsByReference(ref);
//
//            // Send the list of matching objects to the client
//            client.sendEvent("matched_objects", matchedObjects);
//            System.out.println("matchedObjects = " + matchedObjects);
//        });
        server.addEventListener("search_objects",String.class,onSendMessageNew);
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                HandshakeData handshakeData = client.getHandshakeData();
//                String userId = handshakeData.getSingleUrlParam("userId");
                String key = handshakeData.getSingleUrlParam("key"); //blocop
//                log.info("new user connected with id =  " + userId);
                log.info("new user connected with key  =  " + key);
                if (key == null) {
                    // Connected user has ID userId
                    // Do something with the user ID
                } else {
                    if (sessions.containsKey(key)) {
                        List<String> existingList = sessions.get(key);
                        if (!existingList.contains(key)) existingList.add(key);
                    } else {
                        sessions.computeIfAbsent(key, k -> new ArrayList<>()).add(key);
                    }
                    System.out.println("MAP SIZE FOR KEY= " + key + " IS " + sessions.get(key).size());
                }
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                HandshakeData handshakeData = client.getHandshakeData();
                String userId = handshakeData.getSingleUrlParam("userId");
                String key = handshakeData.getSingleUrlParam("key");
                log.warn("USER DISCONNECT");
                System.out.println("userId = " + userId);
                System.out.println("key = " + key);
                if (userId != null && key != null) {
                    if (sessions.containsKey(key)) {
                        List<String> existingList = sessions.get(key);
                        existingList.remove(userId);
                        if (existingList.size() == 0) {
                            log.info("This sessions is finished");
                            sessions.remove(key);
                        }
                    }
                }
            }
        });
        server.start();
        return server;
    }


    public DataListener<User> onConnect = (socketIOClient, user, ackRequest) -> {
        System.out.println(socketIOClient.getSessionId());
        log.error(user.getUsername() + " connected");
        ackRequest.sendAckData("You are connected");
    };


    public DataListener<User> onDisConnect = (socketIOClient, user, ackRequest) -> {
        System.out.println(socketIOClient.getSessionId());
        ackRequest.sendAckData("You are disconnected");
        log.error(user.getUsername() + " disconnected");
    };
//    public DataListener<String> onSendMessage = new DataListener<String>() {
//        @Override
//        public void onData(SocketIOClient client, String message, AckRequest acknowledge) throws Exception {
//            Message messageObject = new Gson().fromJson(message, Message.class);
//            log.info(messageObject.getKey());
//
//            // Convert the message to a byte array
//            byte[] messageBytes = message.getBytes();
//
//            // Get the size of the message in bytes
//            int messageSize = messageBytes.length;
//            System.out.println("messageSize = " + messageSize);
//
//            /**
//             * Sending message to target user
//             * target user should subscribe the socket event with his/her name.
//             * Send the same payload to user
//             */
//            List<String> clientId = sessions.get(messageObject.getKey());
//            for (String str : clientId
//            ) {
//                Stream<SocketIOClient> clientStream = server.getAllClients().stream().filter(d ->
//                        d.getHandshakeData().getSingleUrlParam("userId").equals(str));
//                SocketIOClient ioClient;
//                try {
//                    ioClient = clientStream.findAny().get();
//                } catch (NoSuchElementException e) {
//                    e.printStackTrace();
//                    log.error("Client not found !");
//                    return;
//                }
//                HandshakeData handshakeData = ioClient.getHandshakeData();
//                String userId = handshakeData.getSingleUrlParam("userId");
//                String key = handshakeData.getSingleUrlParam("key");
//                System.out.println("------------INFORMATION OF CLIENT ------------------------------------");
//                System.out.println("key = " + key);
//                System.out.println("userID = " + userId);
//                System.out.println("----------------------------------------------------------------------");
//
//            }
//
//            /**
//             * After sending message to target user we can send acknowledge to sender
//             */
//            acknowledge.sendAckData("Message send to target user successfully");
//        }
//    };
    public DataListener<String> onSendMessageNew = new DataListener<String>() {
        @Override
        public void onData(SocketIOClient client, String message, AckRequest acknowledge) throws Exception {
            List<String> clientId = sessions.get(message);
            for (String str : clientId
            ) {
                Stream<SocketIOClient> clientStream = server.getAllClients().stream().filter(d ->
                        d.getHandshakeData().getSingleUrlParam("key").equals(str));
                SocketIOClient ioClient;
                try {
                    ioClient = clientStream.findAny().get();
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                    log.error("Client not found !");
                    return;
                }
                HandshakeData handshakeData = ioClient.getHandshakeData();
                String key = handshakeData.getSingleUrlParam("key");
                System.out.println("------------INFORMATION OF CLIENT ------------------------------------");
                System.out.println("key = " + key);
                System.out.println("----------------------------------------------------------------------");
                List<Evenement> matchedObjects = convertToItem(searchObjectsByReference(key));
                //List<EvenementRedis> matchedObjects = searchObjectsByReference(key);
                ioClient.sendEvent("matched_objects", matchedObjects);
            }
            acknowledge.sendAckData("Message send to target user successfully");
        }
    };


    @PreDestroy
    public void stopSocketIOServer() {
        this.server.stop();
    }

    public List<String> getConnectedUsers(String key) {
        return sessions.get(key);
    }



    //************************************** UPDATE ***************************

    // 1-OLD VERSION
    /*private List<Evenement> searchObjectsByReference(String ref) {
        return evenementDao.findBySalleBlocOperatoirReference(ref);
    }*/

    // 1-NEW VERSION
    private List<EvenementRedis> searchObjectsByReference(String ref) {
        Flux<EvenementRedis> evenementRedisFlux = searchObjectsByReferenceInRedis(ref);

        List<EvenementRedis> evenementRedisList = evenementRedisFlux.collectList().block();

        if (evenementRedisList != null && !evenementRedisList.isEmpty()) {
            return ImmutableList.copyOf(evenementRedisList);
        } else {
            List<EvenementRedis> evenementRedis = retrieveFromDatabase(ref);
            if (!evenementRedis.isEmpty()) {
                Map<String, List<EvenementRedis>> evenementRedisMap = new HashMap<>();
                evenementRedisMap.put(ref, evenementRedis);
                template.opsForHash()
                        .putAll(ref, evenementRedisMap)
                        .block();
                return evenementRedis;
            } else {
                return Collections.emptyList();
            }
        }
    }
    //2- ASSOCIATED MODIF


    private List<EvenementRedis> convert(List<Evenement> evenementList) {
        List<EvenementRedis> res = new ArrayList<>();
        if (evenementList != null) {
            for (Evenement evenement : evenementList) {
                res.add(evenementRedisConverter.toDto(evenement));
            }
        }
        return res;
    }

    private List<Evenement> convertToItem(List<EvenementRedis> evenementRediss) {
        List<Evenement> res = new ArrayList<>();
        if (evenementRediss != null) {
            for (EvenementRedis evenementRedis : evenementRediss) {
                res.add(evenementRedisConverter.toItem(evenementRedis));
            }
        }
        return res;
    }

    private List<EvenementRedis> retrieveFromDatabase(String ref) {
        List<Evenement> evenementList = evenementDao.findBySalleBlocOperatoirReference(ref);
        List<EvenementRedis> evenementRedisList = convert(evenementList);

        if (!evenementRedisList.isEmpty()) {
            for (EvenementRedis evenementRedis : evenementRedisList) {
                template.opsForHash()
                        .put(ref, String.valueOf(evenementRedis.getReference()), evenementRedis)
                        .subscribe();
            }
        }

        return evenementRedisList;
    }



    public Flux<EvenementRedis> searchObjectsByReferenceInRedis(String reference) {
        return template.opsForHash().get(String.valueOf(reference), reference)
                .flux()
                .map(object -> (EvenementRedis) object);
    }
    @Autowired
    private EvenementRedisConverter evenementRedisConverter;
    @Autowired
    private ReactiveRedisTemplate<String, EvenementRedis> template;


}
