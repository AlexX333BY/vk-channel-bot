# vk-channel-bot
VK bot for publishing wall posts and sending it to channels

How to compile:
* Simply run fatJar task with Gradle

How to start:
1. Create VK app [here](https://vk.com/editapp?act=create)
2. Get app ID and client secret key of this application
3. Run program through console with -i key for app ID and -k key for client secret. For example:
	>java -jar vk-moderator-bot-1.0.jar -i 1 -k abcd1234
4. You should be redirected to your browser. Give the permissions and get "code" value from browser line
5. Paste it into program
6. Then program will ask you for your VK ID to consider admin, chat ID to listen for materials and wall ID to post on. They will be stored at ./data/config.cfg later

Command list:
* `/subscribe` - set chat to be counted as channel-bot
* `/unsubscribe` - remove chat from channel list
* `/test` - test if bot is running
* `/shutdown` - shutdown app