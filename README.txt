Le dossier Rendu contient deux projets : 

-Le premier, "Word2Vec" contient le code permettant de générer la représentation vectorielle des mots à l'aide d'un bibliothèque Java. Le chemin d'accès aux données n'est cependant pas bon car il pointe vers un dossier Utilisateur d'une autre machine.

-Le second, "ProjetTempsReel" contient le Chatbot à proprement dit. Pour l'utiliser, il faut run la classe "ServerUI" une fois et la classe "ClientUI" autant de fois que souhaitée. Pour la connexion, deux comptes ont été définis actuellement :
	• Nom d'utilisateur 1 : charon
	• Mot de passe 1 : insa

	• Nom d'utilisateur 2 : insa
	• Mot de passe 2 : INSA
Ils permettent de comparer ce qui a été déjà reservé par l'autre ou non.
L'agenda est également fourni et il dispose de quelques réservations déjà enregistrées, mais il est modifiable, et valable jusqu'au 6 avril.
Dans l'interface du serveur, on peut d'abord choisir le dictionnaire à utiliser avant de le lancer, et une fois qu'il est démarré, on peut choisir quel algorithme doit être utilisé pour classifier les messages reçus.
Ce projet contient également le fichier "knn_references.txt" qui contient l'ensemble des phrases de référence utilisées pour classer les messages.