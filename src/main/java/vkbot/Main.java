package vkbot;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

    private static final String DEFAULT_REDIRECT_URI = "https://oauth.vk.com/blank.html";
    private static final String API_VERSION = "5.74";
    private static final String SCOPE = "photos,audio,video,messages,wall,offline,docs";

    private static String getCode(int appId, String redirectUri, String scope, String apiVersion) {
        String codeUrl = String.format("https://oauth.vk.com/authorize?client_id=%d&display=page&redirect_uri=%s&scope=%s&response_type=code&v=%s",
                appId, redirectUri, scope, apiVersion);
        System.out.println("Please enter \"code\" value from browser address line");
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(codeUrl));
            } catch (URISyntaxException | IOException ex) {
                System.out.format("You can get it here:\n\t%s", codeUrl);
            }
        } else {
            System.out.format("You can get it here:\n\t%s", codeUrl);
        }
        return new Scanner(System.in).nextLine();
    }

    private static int getIntFromConsole(String message) {
        Scanner scanner = new Scanner(System.in);
        System.out.format("%s: ", message);
        return scanner.nextInt();
    }

    public static void main(String[] args) {

        Option apiIdOption = new Option("i", "id", true, "APP ID of VK application");
        apiIdOption.setRequired(true);
        Option clientSecretOption = new Option("k", "key", true, "Client secret key of VK application");
        clientSecretOption.setRequired(true);
        Option redirectUriOption = new Option("u", "uri", true, "Redirect uri of VK application");
        redirectUriOption.setRequired(false);
        Option codeOption = new Option("c", "code", true, "Authorization code of VK user. More: https://vk.com/dev/authcode_flow_user");
        codeOption.setRequired(false);

        Options options = new Options();
        options.addOption(apiIdOption);
        options.addOption(clientSecretOption);
        options.addOption(redirectUriOption);
        options.addOption(codeOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar " + System.getProperty("java.class.path"), options);
            return;
        }

        int appId = Integer.parseInt(cmd.getOptionValue(apiIdOption.getOpt()));
        String clientSecret = cmd.getOptionValue(clientSecretOption.getOpt()),
                redirectUri = cmd.getOptionValue(redirectUriOption.getOpt(), DEFAULT_REDIRECT_URI),
                code;

        if (cmd.hasOption(codeOption.getOpt())) {
            code = cmd.getOptionValue(codeOption.getOpt());
        } else {
            code = getCode(appId, redirectUri, SCOPE, API_VERSION);
        }

        try {
            VkChannelBot bot = new VkChannelBot(appId, clientSecret, redirectUri, code);
            try {
                bot.parseConfigFile();
            } catch (IOException e) {
                System.err.println("Error parsing config file: " + e.getMessage());
            }
            bot.authorize();
            boolean shouldRewrite = false;
            while (bot.getAdminId() < 1) {
                bot.setAdminId(getIntFromConsole("Print admin ID"));
                shouldRewrite = true;
            }
            while (bot.getChatToListenId() < 1) {
                bot.setChatToListenId(getIntFromConsole("Print chat ID to listen for"));
                shouldRewrite = true;
            }
            while (bot.getCommunityId() > -1) {
                bot.setCommunityId(getIntFromConsole("Print community ID to post on wall"));
                shouldRewrite = true;
            }
            if (shouldRewrite) {
                bot.rewriteConfigFile();
            }
            bot.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }

    }

}
