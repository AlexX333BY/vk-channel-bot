package vkbot;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.audio.Audio;
import com.vk.api.sdk.objects.docs.Doc;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.messages.MessageAttachment;
import com.vk.api.sdk.objects.messages.MessageAttachmentType;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.video.Video;
import com.vk.api.sdk.queries.groups.GroupField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VkModeratorBot extends VkBotCore {

    static final String DEFAULT_CONFIG_FILE_NAME = "data" + System.getProperty("file.separator") + "config.cfg";
    static final String DEFAULT_SUCCESS_MESSAGE = "OK";
    static final String DEFAULT_FAILURE_MESSAGE = "FAIL";
    static final String DEFAULT_ADD_CHAT_QUERY = "/subscribe";
    static final String DEFAULT_REMOVE_CHAT_QUERY = "/unsubscribe";
    static final String DEFAULT_TEST_QUERY = "/test";
    static final String DEFAULT_SHUTDOWN_QUERY = "/shutdown";
    static final String DEFAULT_UNKNOWN_COMMAND_MESSAGE = "Unknown or illegal command";

    static final String ADMIN_ID_CONFIG = "ADMIN_ID";
    static final String LISTEN_CHAT_ID_CONFIG = "LISTEN_CHAT_ID";
    static final String COMMUNITY_ID_CONFIG = "COMMUNITY_ID";
    static final String RESEND_CHAT_ID_CONFIG = "RESEND_CHAT_ID";

    int adminId;
    int communityId;
    String communityName;
    int chatToListenId;
    List<Integer> resendChatIds;
    String configFileName;

    String testQuery;
    String addChatQuery;
    String removeChatQuery;
    String successMessage;
    String shutdownQuery;
    String failureMessage;
    String unknownCommandMessage;

    boolean shouldRun;

    public void setUnknownCommandMessage(String unknownCommandMessage) {
        this.unknownCommandMessage = unknownCommandMessage;
    }

    public String getUnknownCommandMessage() {
        return unknownCommandMessage;
    }

    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }

    public String getTestQuery() {
        return testQuery;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setAddChatQuery(String addChatQuery) {
        this.addChatQuery = addChatQuery;
    }

    public String getAddChatQuery() {
        return addChatQuery;
    }

    public void setRemoveChatQuery(String removeChatQuery) {
        this.removeChatQuery = removeChatQuery;
    }

    public String getRemoveChatQuery() {
        return removeChatQuery;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public List<Integer> getResendChatIds() {
        return resendChatIds;
    }

    public void setResendChatIds(List<Integer> resendChatIds) {
        this.resendChatIds = resendChatIds;
    }

    public void clearResendChatIds() {
        resendChatIds.clear();
    }

    public void addChatIdToResendList(Integer chatId) {
        if (!resendChatIds.contains(chatId)) {
            resendChatIds.add(chatId);
        }
    }

    public void removeChatIdFromResendList(Integer chatId) {
        if (resendChatIds.contains(chatId)) {
            resendChatIds.remove(chatId);
        }
    }

    public int getChatToListenId() {
        return chatToListenId;
    }

    public void setChatToListenId(int chatToListenId) {
        this.chatToListenId = chatToListenId;
    }

    public int getCommunityId() {
        return communityId;
    }

    public void setCommunityId(int communityId) {
        this.communityId = communityId;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public void parseConfigFile() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFileName)))) {
            String line;
            Pattern adminIdPattern = Pattern.compile(String.format("^%s=(\\d+)$", ADMIN_ID_CONFIG)),
                    chatToListenIdPattern = Pattern.compile(String.format("^%s=(\\d+)$", LISTEN_CHAT_ID_CONFIG)),
                    communityIdPattern = Pattern.compile(String.format("^%s=(-\\d+)$", COMMUNITY_ID_CONFIG)),
                    resendChatIdPattern = Pattern.compile(String.format("^%s=(\\d+)$", RESEND_CHAT_ID_CONFIG));
            Matcher matcher;
            while ((line = reader.readLine()) != null) {
                matcher = adminIdPattern.matcher(line);
                if (matcher.matches()) {
                    setAdminId(Integer.parseInt(matcher.group(1)));
                }
                matcher = chatToListenIdPattern.matcher(line);
                if (matcher.matches()) {
                    setChatToListenId(Integer.parseInt(matcher.group(1)));
                }
                matcher = communityIdPattern.matcher(line);
                if (matcher.matches()) {
                    setCommunityId(Integer.parseInt(matcher.group(1)));
                }
                matcher = resendChatIdPattern.matcher(line);
                if (matcher.matches()) {
                    addChatIdToResendList(Integer.parseInt(matcher.group(1)));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing config file: " + e.getMessage());
        }
    }

    void checkResendChats() {
        List<Integer> newChatList = new ArrayList<>();
        for (Integer chatId : resendChatIds) {
            try {
                if (!vk.messages().getChat(actor).chatId(chatId).execute().kicked()) {
                    newChatList.add(chatId);
                }
            } catch (Exception e) {
                // transport or another error
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                //
            }
        }
        resendChatIds = newChatList;
        rewriteConfigFile();
    }

    void rewriteConfigFile() {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(new File(configFileName)))) {
            writer.format("%s=%d\n%s=%d\n%s=%d\n", ADMIN_ID_CONFIG, adminId, LISTEN_CHAT_ID_CONFIG, chatToListenId, COMMUNITY_ID_CONFIG, communityId);
            for (int chatId : resendChatIds) {
                writer.format("%s=%d\n", RESEND_CHAT_ID_CONFIG, chatId);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error rewriting config file: " + e.getMessage());
        }
    }

    enum MessageType {
        ADD_RESEND_CHAT, REMOVE_RESEND_CHAT, TEST, SHUTDOWN, POST, UNKNOWN_OR_ILLEGAL_COMMAND, NOT_A_COMMAND
    }

    MessageType getMessageType(Message message) {
        String text = message.getBody().trim();
        Integer userId = message.getUserId(),
                chatId = message.getChatId();
        if ((text.startsWith(addChatQuery)) && (chatId != null)) {
            return MessageType.ADD_RESEND_CHAT;
        }
        if ((text.startsWith(removeChatQuery)) && (chatId != null)) {
            return MessageType.REMOVE_RESEND_CHAT;
        }
        if (text.startsWith(testQuery)) {
            return MessageType.TEST;
        }
        if ((text.startsWith(shutdownQuery)) && (userId != null) && (userId == adminId)) {
            return MessageType.SHUTDOWN;
        }
        if ((chatId != null) && (chatId == chatToListenId)) {
            return MessageType.POST;
        }
        if (text.startsWith("/")) {
            return MessageType.UNKNOWN_OR_ILLEGAL_COMMAND;
        }
        return MessageType.NOT_A_COMMAND;
    }

    String getPhotoAddress(Photo photo) {
        return "photo" + photo.getOwnerId() + "_" + photo.getId() + ((photo.getAccessKey() == null) ? "" : "_" + photo.getAccessKey());
    }

    String getVideoAddress(Video video) {
        return "video" + video.getOwnerId() + "_" + video.getId() + ((video.getAccessKey() == null) ? "" : "_" + video.getAccessKey());
    }

    String getDocAddress(Doc doc) {
        return "doc" + doc.getOwnerId() + "_" + doc.getId() + ((doc.getAccessKey() == null) ? "" : "_" + doc.getAccessKey());
    }

    String getAudioAddress(Audio audio) {
        return "audio" + audio.getOwnerId() + "_" + audio.getId() + ((audio.getAccessKey() == null) ? "" : "_" + audio.getAccessKey());
    }

    String createTags(List<MessageAttachmentType> types) {
        String tags = new String();
        for (MessageAttachmentType type : types) {
            tags += (" #" + type.toString().toLowerCase() + "@" + communityName);
        }
        return tags.trim();
    }

    String createMessageText(Message message) {
        String text = (message.getBody().isEmpty() ? null : message.getBody());
        return text;
    }

    String createPostText(String messageText, String tags) {
        if ((messageText == null) || messageText.isEmpty()) {
            if ((tags == null) || (tags.isEmpty())) {
                return null;
            } else {
                return tags;
            }
        } else {
            if ((tags == null) || (tags.isEmpty())) {
                return messageText;
            } else {
                return String.format("%s\n\n%s", messageText, tags);
            }
        }
    }

    String getPostAttachment(MessageAttachment attachment) throws ClientException, ApiException {
        switch (attachment.getType()) {
            case PHOTO:
                Photo saved = savePhoto(attachment.getPhoto());
                return getPhotoAddress(saved);
            case DOC:
                return getDocAddress(attachment.getDoc());
            case AUDIO:
                return getAudioAddress(attachment.getAudio());
            case VIDEO:
                return getVideoAddress(attachment.getVideo());
            default:
                return null;
        }
    }

    String getMessageAttachment(MessageAttachment attachment) {
        switch (attachment.getType()) {
            case PHOTO:
                return getPhotoAddress(attachment.getPhoto());
            case DOC:
                return getDocAddress(attachment.getDoc());
            case AUDIO:
                return getAudioAddress(attachment.getAudio());
            case VIDEO:
                return getVideoAddress(attachment.getVideo());
            default:
                return null;
        }
    }

    void processPostMessage(Message message) {
        List<String> postAttachments = new ArrayList<>(),
                messageAttachments = new ArrayList<>();
        List<MessageAttachment> attachments = message.getAttachments();
        List<MessageAttachmentType> types = new ArrayList<>();
        if (attachments != null) {
            for (MessageAttachment attachment : attachments) {
                try {
                    postAttachments.add(getPostAttachment(attachment));
                    messageAttachments.add(getMessageAttachment(attachment));
                    if (!types.contains(attachment.getType())) {
                        types.add(attachment.getType());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing attachment: " + e.getMessage());
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    //
                }
            }
        }
        String tags = createTags(types),
                messageText = createMessageText(message),
                postText = createPostText(messageText, tags);
        if ((messageText != null) || (!postAttachments.isEmpty())) {
            try {
                postOnWall(communityId, postAttachments, postText);
                boolean shouldBeChecked = false;
                for (Integer chatId : resendChatIds) {
                    try {
                        sendMessageToChat(chatId, messageText, messageAttachments);
                    } catch (Exception e) {
                        System.err.println("Error while reposting: " + e.getMessage());
                        shouldBeChecked = true;
                    }
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        //
                    }
                }
                if (shouldBeChecked) {
                    checkResendChats();
                }
            } catch (Exception e) {
                System.err.println("Error while posting: " + e.getMessage());
            }
            try {
                clearPhotoAlbum("saved");
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void processNewMessage(Message message) throws ClientException, ApiException {
        Integer chatId = message.getChatId(),
                userId = message.getUserId();
        switch (getMessageType(message)) {
            case ADD_RESEND_CHAT:
                addChatIdToResendList(chatId);
                checkResendChats();
                sendMessageToChat(chatId, successMessage, null);
                break;
            case REMOVE_RESEND_CHAT:
                removeChatIdFromResendList(chatId);
                checkResendChats();
                sendMessageToChat(chatId, successMessage, null);
                break;
            case TEST:
                if (chatId == null) {
                    sendMessageToUser(userId, successMessage, null);
                } else {
                    sendMessageToChat(chatId, successMessage, null);
                }
                break;
            case SHUTDOWN:
                shouldRun = false;
                if (chatId == null) {
                    sendMessageToUser(userId, successMessage, null);
                } else {
                    sendMessageToChat(chatId, successMessage, null);
                }
                break;
            case POST:
                processPostMessage(message);
                break;
            case UNKNOWN_OR_ILLEGAL_COMMAND:
                if (chatId == null) {
                    sendMessageToUser(userId, "Unknown or illegal command", null);
                } else {
                    sendMessageToChat(chatId, "Unknown or illegal command", null);
                }
                break;
        }
    }

    public void start() {
        List<Message> messages;
        shouldRun = true;
        try {
            communityName = vk.groups().getById(actor).groupId(Integer.toString(Math.abs(communityId)))
                    .fields(GroupField.SCREEN_NAME).execute().get(0).getScreenName();
        } catch (Exception e) {
            System.out.println("Error getting community name: " + e.getMessage());
        }
        while (shouldRun) {
            try {
                messages = getUnreadMessages(MAX_GET_MESSAGES_COUNT, true);
                for (Message message : messages) {
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        //
                    }
                    try {
                        processNewMessage(message);
                    } catch (Exception e) {
                        System.err.println("Error processing new message: " + e.getMessage());
                    }
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    //
                }
            } catch (Exception e) {
                System.err.println("Error getting messages: " + e.getMessage());
            }
        }
    }

    public VkModeratorBot() {
        super();
        adminId = 0;
        communityId = 0;
        chatToListenId = 0;
        resendChatIds = new ArrayList<>();
        configFileName = DEFAULT_CONFIG_FILE_NAME;
        addChatQuery = DEFAULT_ADD_CHAT_QUERY;
        removeChatQuery = DEFAULT_REMOVE_CHAT_QUERY;
        successMessage = DEFAULT_SUCCESS_MESSAGE;
        testQuery = DEFAULT_TEST_QUERY;
        shutdownQuery = DEFAULT_SHUTDOWN_QUERY;
        failureMessage = DEFAULT_FAILURE_MESSAGE;
        unknownCommandMessage = DEFAULT_UNKNOWN_COMMAND_MESSAGE;
    }

    public VkModeratorBot(int appId, String clientSecret, String redirectUri, String code) {
        super(appId, clientSecret, redirectUri, code);
        adminId = 0;
        communityId = 0;
        chatToListenId = 0;
        resendChatIds = new ArrayList<>();
        configFileName = DEFAULT_CONFIG_FILE_NAME;
        addChatQuery = DEFAULT_ADD_CHAT_QUERY;
        removeChatQuery = DEFAULT_REMOVE_CHAT_QUERY;
        successMessage = DEFAULT_SUCCESS_MESSAGE;
        testQuery = DEFAULT_TEST_QUERY;
        shutdownQuery = DEFAULT_SHUTDOWN_QUERY;
        failureMessage = DEFAULT_FAILURE_MESSAGE;
        unknownCommandMessage = DEFAULT_UNKNOWN_COMMAND_MESSAGE;
    }

}
