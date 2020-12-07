package fr.insa.kern.projet.classifying;

import fr.insa.kern.projet.classifying.Classifier;

//Classe qui permet de faire une classification des messages reçus grâce à la distance de Levenshtein
public class LevenshteinClassifier {

    // Seuil pour considérer que le mot est bien reconnu
    private static final int LEVENSHTEIN_THRESHOLD = 2;
    // LISTES DES MOTS POUR CHAQUE CAS
    private static final String[] WLIST_GREETINGS = {"salut", "bonjour", "hello", "coucou", "bonsoir"};
    private static final String[] WLIST_RESERVATION = {"réserver", "reserver", "réservation", "reservation", "creneau", "créneau", "reserve", "réserve", "reserves", "réserves"};
    private static final String[] WLIST_CONFIRMATION = {"oui", "yes", "tout à fait", "tout a fait", "exactement", "exact", "parfait", "d'acc", "absolument"};
    private static final String[] WLIST_NEGATION = {"non", "pas du tout", "faux"};
    private static final String[] WLIST_INFORMATION = {"information", "informations", "info", "infos", "renseignement", "renseignements", "savoir", "liste", "deja", "déjà"};


    // Renvoie le type supposé du message, en effectuant des comparaisons des minimums des distances de Levenshtein
    public static Classifier.MessageType getMessageType(String message) {
        // Pour chaque type, on va vérifier si la distance est inférieure à la précédente. Si oui, on considère ce type comme le bon
        // En cas d'égalité, l'ordre de priorité est : RESERVATION > INFORMATION > CONFIRMATION > NEGATION > SALUTATIONS > AUTRES
        int minDistance = minimumDistanceFromWordToList(message, WLIST_RESERVATION);
        int distance;
        Classifier.MessageType outputType = Classifier.MessageType.RESERVATION;

        distance = minimumDistanceFromWordToList(message, WLIST_INFORMATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = Classifier.MessageType.INFORMATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_CONFIRMATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = Classifier.MessageType.CONFIRMATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_NEGATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = Classifier.MessageType.NEGATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_GREETINGS);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = Classifier.MessageType.GREETINGS;
        }

        // Si on est bien en dessous du seuil on considère que ce n'est pas un faux positif
        if (minDistance <= LEVENSHTEIN_THRESHOLD) {
            return outputType;
        } else { // Sinon on renvoie le type par défaut
            return Classifier.MessageType.OTHER;
        }
    }

    // Calcule la distance minimum entre un message et une liste de mots
    private static int minimumDistanceFromWordToList(String message, String[] words) {
        int minimum = Integer.MAX_VALUE;
        int distance;
        for (String ref_word : words) { // Pour chaque mot de la liste
            for (String mes_word : message.split(" ")) { // Pour chaque mot du message
                distance = levenshteinDistance(ref_word.toLowerCase(), mes_word.toLowerCase()); // On calcule la distnace de Levenshtein entre les deux
                if (distance <= minimum) { // Si elle est inférieur au minimum, ça devient le nouveau min
                    minimum = distance;
                }
            }
        }
        return minimum;
    }

    // Permet de connaître le nombre de modification à faire dans un mot pour obtenir l'autre
    // Les modfications sont : ajout d'une lettre, suppression d'une lettre et substitution d'une lettre
    // Exemple : la distance entre "chiens" et "niche" est de 5, car il faut supprimer le i, le n et le s de "chiens" et ajouter n et i
    // L'algorithme en langage naturel est disponible sur Wikipédia
    private static int levenshteinDistance(String word1, String word2) {
        int[][] d = new int[word1.length() + 1][word2.length() + 1];
        int cout = 0;
        for (int i = 0; i <= word1.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= word2.length(); j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= word1.length(); i++) {
            for (int j = 1; j <= word2.length(); j++) {
                cout = 0;
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) cout = 1;
                // d[i - 1][j] + 1 : suppression du caractère de word1
                // d[i][j - 1] + 1 : insertion du caractère de word2 dans word1
                // d[i - 1][j - 1] + cout : substitution des deux caractères
                d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + cout));
            }
        }
        return d[word1.length()][word2.length()];
    }

}
