# MensaBot - A bot that reminds you of your food in your canteen

This bot sends you a daily reminder of the current food in your canteen (dt. mensa).
Additionally, you can use commands to request the listing directly.

## Features

* Schedule daily posts about the food in your canteen
* Request summary of food in your mensa (at the current date)
* Change display name per room
* Simple rights management (only configured admins can interact with the bot)

![Functions](.docs/imgs/functions.png)

### Supported APIs
This bot aims to support multiple canteen APIs. For details about the implementation, take a look at [Development](#development).

Currently, the bot supports the following mensa:
* [Studierendenwerk Karlsruhe](https://www.sw-ka.de/en/hochschulgastronomie/speiseplan/)

## Setup

1. Get a matrix account for the bot (e.g., on your own homeserver or on `matrix.org`)
2. Prepare configuration:
    * Copy `config-sample.json` to `config.json`
    * Enter `baseUrl` to the matrix server and `username` / `password` for the bot user
    * Add yourself to the `admins` (and delete my account from the list :))
    * You can limit the users that can interact with the bot by defining the `users` list
3. Either run the bot via jar or run it via the provided docker.
    * If you run it locally, you can use the environment variable `CONFIG_PATH` to point at your `config.json` (defaults to `./config.json`)
    * If you run it in docker, you can use a command similar to this `docker run -itd -v $LOCAL_PATH_TO_CONFIG:/usr/src/bot/data/config.json:ro ghcr.io/dfuchss/mensabot`

### Configuration Options

The `config.json` file contains all configuration options for the bot. Here's a detailed explanation of each option:

#### Required Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `baseUrl` | string | The base URL of the Matrix server the bot should connect to (e.g., `"https://matrix-client.matrix.org"`) |
| `username` | string | The username of the bot's Matrix account |
| `password` | string | The password of the bot's Matrix account |
| `admins` | array of strings | List of Matrix user IDs that have admin privileges (e.g., `["@user:matrix.org"]`) |

#### Optional Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `prefix` | string | `"mensa"` | The command prefix the bot listens to. Commands will be invoked with `!<prefix> <command>` |
| `dataDirectory` | string | `"./data/"` | Path to the directory where the bot stores databases and media files |
| `timeToSendUpdates` | string | - | The time when the bot sends daily meal updates to subscribed rooms (format: "HH:MM", e.g., `"07:30"`) |
| `users` | array of strings | `[]` | List of Matrix server domains or user IDs that are allowed to interact with the bot. If empty, all users can interact. Use domains like `":matrix.org"` to allow all users from a server |
| `subscribers` | array of strings | `[]` | List of room IDs that are subscribed to daily meal updates. Rooms must be manually added to this list by an admin after users request subscription via the `!mensa subscribe` command |
| `canteensForSubscribers` | array of strings | `["adenauerring"]` | List of canteen identifiers that the bot provides information about for subscribed rooms |
| `translation` | object | `null` | Configuration for automatic translation of menus (alpha feature) |

#### Translation Configuration (Optional)
If you want to enable automatic translation of menus, add a `translation` object with these options:

* **`ollamaServerUrl`** (string): URL of your Ollama server for translation
* **`ollamaUser`** (string, optional): Username for Ollama server authentication
* **`ollamaPassword`** (string, optional): Password for Ollama server authentication  
* **`model`** (string, default: `"llama3.1:8b"`): The language model to use for translation
* **`prompt`** (string): The prompt template for translation. Use `{}` placeholders for model name and text to translate

Example translation configuration:
```json
"translation": {
    "ollamaServerUrl": "http://localhost:11434",
    "model": "llama3.1:8b",
    "prompt": "Translate this menu to English. Keep markdown and emojis. Just start with the markdown, no further output. In the end add 'Translated by {}.'\\n{}"
}
```

#### Example Configuration
```json
{
    "prefix": "mensa",
    "baseUrl": "https://matrix-client.matrix.org",
    "username": "your-bot-username",
    "password": "your-bot-password",
    "dataDirectory": "./data/",
    "timeToSendUpdates": "07:30",
    "admins": [
        "@your-username:matrix.org"
    ],
    "users": [
        ":matrix.org",
        ":your-homeserver.org"
    ],
    "subscribers": [],
    "canteensForSubscribers": ["adenauerring"],
    "translation": null
}
```

## Usage

* An admin can invite the bot to an *unencrypted* room. If the room has enabled encryption or if the invite was not sent by an admin, the bot ignores it (without logging it)
* After the bot has joined use `!mensa help` to get an overview about the features of the bot (remember: the bot only respond to admin users)
* In order to get daily notifications about the food in your canteen, simply use `!mensa subscribe`. This command will display the room ID and instructions to contact a bot admin. An admin must then manually add the room ID to the `subscribers` list in the config file and restart the bot.

## Development

I'm typically online in the [Trixnity channel](https://matrix.to/#/#trixnity:imbitbu.de). So feel free to tag me there if you have any questions.

* The bot is build using the [Trixnity](https://trixnity.gitlab.io/trixnity/) framework.
* The basic functionality is located in [Main.kt](src/main/kotlin/org/fuchss/matrix/mensa/Main.kt). There you can also find the main method of the program.
* Every canteen that shall be considered has to implement the `CanteenAPI`. Currently, there is only one implementation for
  the [sw-ka.de interface](https://sw-ka.de).
