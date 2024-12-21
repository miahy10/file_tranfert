# Projet de Transfert de Fichiers

## Introduction

Ce projet consiste en une architecture client-serveur pour le transfert de fichiers, où le client peut envoyer, recevoir, lister et supprimer des fichiers d'un serveur principal qui gère plusieurs sous-serveurs. Le projet marche avec des lignes de commande comme les commandes Linux .

## Architecture

Le projet utilise une architecture client-serveur, où le serveur principal écoute les connexions des clients et gère les opérations de fichiers, y compris la distribution des fichiers aux sous-serveurs.

## Structure des Fichiers

- `Client.java`: Implémente la logique côté client, permettant aux utilisateurs d'interagir avec le serveur via des commandes.
- `MainServer.java`: Implémente le serveur principal qui gère les connexions des clients et les opérations de fichiers.
- `SubServer.java`: Implémente les sous-serveurs qui stockent et récupèrent des fragments de fichiers.
- `ConfigLoader.java`: Charge les paramètres de configuration à partir d'un fichier `config.txt`.

## Configuration

Le projet peut être configuré à l'aide du fichier `config.txt`, qui contient les paramètres suivants :

- **main_server.port**: Le port sur lequel le serveur principal écoute les connexions entrantes (par défaut : 12345).
- **main_server.directory**: Le répertoire où le serveur principal stocke les fichiers (par défaut : `server_directory`).
- **chunk_size**: La taille des morceaux de fichiers utilisés lors du transfert (par défaut : 1024 octets).
- **sub_server.count**: Le nombre de sous-serveurs configurés (par défaut : 3).
- **sub_server.1.host**: L'adresse hôte du premier sous-serveur (par défaut : `127.0.0.1`).
- **sub_server.1.port**: Le port du premier sous-serveur (par défaut : 12346).
- **sub_server.2.host**: L'adresse hôte du deuxième sous-serveur (par défaut : `127.0.0.1`).
- **sub_server.2.port**: Le port du deuxième sous-serveur (par défaut : 12347).
- **sub_server.3.host**: L'adresse hôte du troisième sous-serveur (par défaut : `127.0.0.1`).
- **sub_server.3.port**: Le port du troisième sous-serveur (par défaut : 12348).

## Utilisation

Voici quelques exemples de commandes que vous pouvez utiliser avec le client :

- **GET**: Récupérer un fichier du serveur. Cette commande envoie une requête au serveur pour obtenir un fichier spécifique, qui est ensuite reconstruit et copié vers le chemin de destination spécifié.
- **PUT**: Envoyer un fichier au serveur. Cette commande permet à l'utilisateur d'envoyer un fichier au serveur, qui le stocke pour un accès ultérieur.
- **LS**: Lister les fichiers disponibles. Cette commande interroge le serveur pour obtenir une liste des fichiers actuellement stockés.
- **RM**: Supprimer un fichier du serveur. Cette commande permet à l'utilisateur de supprimer un fichier spécifique du serveur.
  Voici quelques exemples de commandes que vous pouvez utiliser avec le client :
- **GET**: Récupérer un fichier du serveur. Cette commande envoie une requête au serveur pour obtenir un fichier spécifique, qui est ensuite reconstruit et copié vers le chemin de destination spécifié.
- **PUT**: Envoyer un fichier au serveur. Cette commande permet à l'utilisateur d'envoyer un fichier au serveur, qui le stocke pour un accès ultérieur.
- **LS**: Lister les fichiers disponibles. Cette commande interroge le serveur pour obtenir une liste des fichiers actuellement stockés.
- **RM**: Supprimer un fichier du serveur. Cette commande permet à l'utilisateur de supprimer un fichier spécifique du serveur.

### Instructions de démarrage

Pour tous: compiler les fichiers .java avant de commencer avec `javac -d . *.java`

- NB: Pour lancer le serveur principal et les sous-serveurs, exécutez les commandes dans différents terminal. Les sous-serveurs doivent être lancés avant le serveur principal.
- Pour lancer le(les) sous-serveur(s), exécutez `java SubServer.java` (ou `java SubServer.java <port dans le config.txt>`)
- Pour lancer le serveur principal : `java MainServer.java` (ou `java MainServer.java <port dans le config.txt>`)
- Pour lancer l'interface client, exécutez la commande `java Client.java`

### Fonctionnalités des Fichiers

#### Côté Client

- **Client.java**: Implémente la logique côté client, permettant aux utilisateurs d'interagir avec le serveur via des commandes. Il gère les commandes de l'utilisateur, envoie et reçoit des fichiers, liste les fichiers disponibles et supprime des fichiers. Les principales méthodes incluent :
  - **main(String[] args)**: Point d'entrée de l'application client, gère les commandes de l'utilisateur.
  - **sendFile(String filePath)**: Envoie un fichier au serveur en utilisant un socket pour établir la connexion.
  - **receiveFile(String fileName, String destinationPath)**: Récupère un fichier du serveur et le sauvegarde à l'emplacement spécifié, en s'assurant que le fichier est reconstruit à son état d'origine.
  - **listFiles()**: Liste les fichiers disponibles sur le serveur.
  - **removeFile(String fileName)**: Supprime un fichier du serveur.
    Voici quelques exemples de commandes que vous pouvez utiliser avec le client :
- **GET**: Récupérer un fichier du serveur. Cette commande envoie une requête au serveur pour obtenir un fichier spécifique, qui est ensuite reconstruit et copié vers le chemin de destination spécifié.
- **PUT**: Envoyer un fichier au serveur. Cette commande permet à l'utilisateur d'envoyer un fichier au serveur, qui le stocke pour un accès ultérieur.
- **LS**: Lister les fichiers disponibles. Cette commande interroge le serveur pour obtenir une liste des fichiers actuellement stockés.
- **RM**: Supprimer un fichier du serveur. Cette commande permet à l'utilisateur de supprimer un fichier spécifique du serveur.

### Fonctionnalités des Fichiers

#### Côté Client

- **Client.java**: Implémente la logique côté client, permettant aux utilisateurs d'interagir avec le serveur via des commandes. Il gère les commandes de l'utilisateur, envoie et reçoit des fichiers, liste les fichiers disponibles et supprime des fichiers. Les principales méthodes incluent :
  - **main(String[] args)**: Point d'entrée de l'application client, gère les commandes de l'utilisateur.
  - **sendFile(String filePath)**: Envoie un fichier au serveur en utilisant un socket pour établir la connexion.
  - **receiveFile(String fileName, String destinationPath)**: Récupère un fichier du serveur et le sauvegarde à l'emplacement spécifié, en s'assurant que le fichier est reconstruit à son état d'origine.
  - **listFiles()**: Liste les fichiers disponibles sur le serveur.
  - **removeFile(String fileName)**: Supprime un fichier du serveur.

#### Côté Serveur

- **ConfigLoader.java**: Implémente la logique pour charger les paramètres de configuration à partir du fichier `config.txt`. Les principales méthodes incluent :

  - **ConfigLoader()**: Charge les paramètres de configuration à partir du fichier `config.txt`.
  - **get(String key)**: Récupère une valeur de propriété en tant que chaîne.
  - **getInt(String key)**: Récupère une valeur de propriété en tant qu'entier.

- **MainServer.java**: Implémente le serveur principal qui gère les connexions des clients et les opérations de fichiers. Il traite les commandes envoyées par les clients, distribue les fichiers aux sous-serveurs pour le stockage, et assemble les fichiers à partir de leurs fragments lorsque nécessaire. Les principales méthodes incluent :

  - **main(String[] args)**: Point d'entrée du serveur principal, gère les connexions des clients.
  - **handleClient(Socket clientSocket)**: Traite les commandes des clients.
  - **distributeAndReplicateFile(DataInputStream dis, String fileName, long fileSize)**: Distribue un fichier aux sous-serveurs.
  - **assembleFile(String fileName)**: Assemble un fichier à partir de ses fragments.
  - **getActiveSubServers()**: Vérifie quels sous-serveurs sont en ligne.

- **SubServer.java**: Implémente les sous-serveurs qui stockent et récupèrent des fragments de fichiers. Chaque sous-serveur gère les opérations de fichiers au niveau local, y compris la réception, l'envoi et la suppression de fichiers. Les principales méthodes incluent :
  - **main(String[] args)**: Point d'entrée de l'application sous-serveur.
  - **receiveFile(DataInputStream dis, File file, long fileSize)**: Reçoit un fichier et le sauvegarde.
  - **sendFile(DataOutputStream dos, File file)**: Envoie un fichier à un client ou au serveur principal.
  - **listFiles()**: Liste les fichiers stockés dans le sous-serveur.
  - **deleteFile(String fileName)**: Supprime les fragments de fichiers associés à un nom de fichier.
