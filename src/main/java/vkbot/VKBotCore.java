package vkbot;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.docs.Doc;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.video.Video;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;
import com.vk.api.sdk.queries.wall.WallPostQuery;

import java.util.ArrayList;
import java.util.List;

public class VKBotCore {

    int appId;
    String clientSecret;
    String redirectUri;
    String code;
    UserActor actor;
    VkApiClient vk;

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public int getAppId() {
        return appId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void sendMessageToUser(int userId, String message, List<String> attachments) throws ClientException, ApiException {
        MessagesSendQuery query = vk.messages().send(actor).userId(userId);
        if (message != null) {
            query = query.message(message);
        }
        if (attachments != null) {
            query = query.attachment(attachments);
        }
        query.execute();
    }

    public void sendMessageToChat(int chatId, String message, List<String> attachments) throws ClientException, ApiException {
        MessagesSendQuery query = vk.messages().send(actor).chatId(chatId);
        if (message != null) {
            query = query.message(message);
        }
        if ((attachments != null) && (!attachments.isEmpty())) {
            query = query.attachment(attachments);
        }
        query.execute();
    }

    public List<Message> getUnreadMessages(int count, boolean markAsRead) throws ClientException, ApiException {
        List<Message> messages = vk.messages().get(actor).count(count).execute().getItems();
        List<Message> unreadMessages = new ArrayList<>();
        for (Message message : messages) {
            if (!message.isReadState()) {
                unreadMessages.add(message);
            }
        }
        if (markAsRead) {
            List<Integer> ids = new ArrayList<>();
            for (Message message : unreadMessages) {
                ids.add(message.getId());
            }
            if (!ids.isEmpty()) {
                vk.messages().markAsRead(actor).messageIds(ids).execute();
            }
        }
        return unreadMessages;
    }

    public void postOnWall(int wallId, List<String> attachments, String message) throws ClientException, ApiException {
        WallPostQuery query = vk.wall().post(actor).ownerId(wallId).fromGroup(true).signed(false);
        if ((attachments != null) && (!attachments.isEmpty())) {
            query.attachments(attachments);
        }
        if (message != null) {
            query.message(message);
        }
        query.execute();
    }

    public Photo savePhoto(Photo photo) throws ClientException, ApiException {
        vk.photos().copy(actor, photo.getOwnerId(), photo.getId()).accessKey(photo.getAccessKey()).execute();
        List<Photo> photos = vk.photos().get(actor).albumId("saved").execute().getItems();
        return photos.get(photos.size() - 1);
    }

    public void clearPhotoAlbum(String albumId) throws ClientException, ApiException {
        List<Photo> photos = vk.photos().get(actor).albumId(albumId).execute().getItems();
        for (Photo photo : photos) {
            try {
                deletePhoto(photo);
            } catch (Exception e) {

            }
        }
    }

    public void deletePhoto(Photo photo) throws ClientException, ApiException {
        vk.photos().delete(actor, photo.getId()).execute();
    }

    public Video saveVideo(Video video) throws ClientException, ApiException {
        vk.videos().add(actor, video.getId(), video.getOwnerId()).execute();
        return vk.videos().get(actor).count(1).execute().getItems().get(0);
    }

    public void deleteVideo(Video video) throws ClientException, ApiException {
        vk.videos().delete(actor, video.getId()).execute();
    }

    public Doc saveDoc(Doc doc, String title) throws ClientException, ApiException {
        vk.docs().add(actor, doc.getOwnerId(), doc.getId()).execute();
        Doc savedDoc = vk.docs().get(actor).count(1).execute().getItems().get(0);
        vk.docs().edit(actor, savedDoc.getOwnerId(), savedDoc.getId()).title(title).execute();
        return savedDoc;
    }

    public void deleteDoc(Doc doc) throws ClientException, ApiException {
        vk.docs().delete(actor, doc.getOwnerId(), doc.getId()).execute();
    }

    public void authorize() throws ClientException, ApiException {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        UserAuthResponse authResponse = vk.oauth()
                .userAuthorizationCodeFlow(appId, clientSecret, redirectUri, code)
                .execute();
        actor = new UserActor(authResponse.getUserId(), authResponse.getAccessToken());
    }

    public VKBotCore() {
        appId = 0;
        clientSecret = null;
        redirectUri = null;
        code = null;
    }

    public VKBotCore(int appId, String clientSecret, String redirectUri, String code) {
        this.appId = appId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.code = code;
    }

}
