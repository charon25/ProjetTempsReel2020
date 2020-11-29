package fr.insa.kern.projet;

import java.util.ArrayList;
import java.util.regex.*;

public class Classifier {

    // Enumeration qui permet de classer les messages reçus
    public enum MessageType {GREETINGS, RESERVATION, CONFIRMATION, NEGATION, INFORMATION, OTHER}

    // Seuil pour considérer que le mot est bien reconnu
    private static final int LEVENSHTEIN_THRESHOLD = 2;
    // Mots à supprimer, comme les prépositions et déterminants
    private static final String[] WORDS_TO_REMOVE = {"le", "la", "les", "un", "une", "des", "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "nos", "votre", "vos", "leur", "leurs", "à", "y", "du", "de", "ou", "où", "en", "au", "dès", "par", "sur", "sûr", "vers", "pour", "sans"};
    // Symboles de ponctuation à retirer
    private static final String[] PUNCTATION = {"?", "!", ".", ",", ";", "=", "'", "\"", ":"};
    // Mois en toute lettre (de 0 à 11)
    public static final String[] MONTHS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

    // LISTES DES MOTS POUR CHAQUE CAS
    private static final String[] WLIST_GREETINGS = {"salut", "bonjour", "hello", "coucou", "bonsoir"};
    private static final String[] WLIST_RESERVATION = {"réserver", "reserver", "réservation", "reservation", "creneau", "créneau", "reserve", "réserve", "reserves", "réserves"};
    private static final String[] WLIST_CONFIRMATION = {"oui", "yes", "tout à fait", "tout a fait", "exactement", "exact", "parfait", "d'acc", "absolument"};
    private static final String[] WLIST_NEGATION = {"non", "pas du tout", "faux"};
    private static final String[] WLIST_INFORMATION = {"information", "informations", "info", "infos", "renseignement", "renseignements", "savoir", "liste", "deja", "déjà"};

    // REPONSE DU CHATBOT SELON LE CAS (les %nom% sont remplacés dans le code)
    private static final String ANSWER_GREETING = "Bonjour.";
    private static final String ANSWER_RESERVATION = "Créneau souhaité : %hourStart%h-%hourEnd%h le %day% %month% . Est-ce correct ?";
    private static final String[] ANSWER_RESERVATION_ERRORS = {"Le créneau souhaité est au-delà de la date limite du calendrier. Veuillez en choisir un autre.", "Le créneau souhaité est dans le passé. Veuillez en choisir un autre.", "Le créneau souhaité est déjà réservé. Veuillez en choisir un autre."};
    private static final String ANSWER_RESERVATION_MISSING = "Impossible de récupérer l'heure ou la date du rendez-vous souhaité. Merci de les renvoyer.";
    private static final String ANSWER_CONFIRMATION_FAILED = "Une erreur est survenue. Merci de vérfier que le créneau est libre.";
    private static final String ANSWER_CONFIRMATION = "Créneau correctement réservé.";
    private static final String ANSWER_NEGATION = "Demande annulée.";
    private static final String ANSWER_INFORMATION = "Les créneaux déjà occupés sont : // %info%";
    private static final String ANSWER_OTHER = "Impossible de comprendre le message. Merci de le reformuler et de le renvoyer.";

    // Renvoie la réponse dans un tableau d'objet qui contient les infos suivantes (taille minimum 2, optionnelle jusqu'à 6) :
    // 0 : type du message (MessageType)
    // 1 : réponse à renvoyer (String)
    // 2 : indique un bon fonctionnement (boolean) [op]
    // 3 : heure de la réservation (int) [op]
    // 4 : jour de la réservation (int) [op]
    // 5 : mois de la réservation (int) [op]
    // PARAMETRES :
    // user : utilisateur qui fait la demande
    // message : message de cet utilisateur
    // agenda : instance de l'agenda conservée par le serveur
    // reservationDateTime : tableau de string format {"yyyy-mm-dd", h} (mois de 0 à 11)
    // previousIsReservation : true si la demande précédente était un rdv
    public static Object[] getResponse(String user, String message, Agenda agenda, String[] reservationDateTime, boolean previousIsReservation) {
        Object[] output;
        // On met le message en minuscule
        message = message.toLowerCase();
        // On supprime tous les signes de ponctuation
        for (String punctuation : PUNCTATION) {
            message = message.replace(punctuation, "");
        }
        // Pour chaque mot à supprimer, on l'enlève qu'il soit au milieu, au début ou à la fin
        for (String word : WORDS_TO_REMOVE) {
            message = message.replace(" " + word + " ", " "); // Au milieu
            if (message.startsWith(word)) { // Au début
                message = message.substring(word.length());
            }
            if (message.endsWith(word)) { // A la fin
                message = message.substring(0, message.length() - word.length());
            }
        }
        // Enfin on remplace les espaces multiples par une seule, et on enlève les espaces en début et fin de message
        message = message.replaceAll(" +", " ").replaceAll("^ ", "").replaceAll(" $", "");

        // On récupère le type de message
        MessageType type = getMessageType(message);
        // Si le précédent message était un de réservation et que le message actuel contient des informations de date et heure
        // alors on considère que ce message aussi est de réservation
        if (previousIsReservation && extractDateTime(message)[0] == 1) {
            type = MessageType.RESERVATION;
        }
        // Si le message précédent n'était pas de réservation, alors les message d'approbation et de négation sont considérés comme du bruit
        if (!previousIsReservation && (type == MessageType.NEGATION || type == MessageType.CONFIRMATION)) {
            type = MessageType.OTHER;
        }

        switch (type) { // Selon le type de message
            case GREETINGS: // Si ce sont des salutations, on en renvoie
                output = new Object[2];
                output[0] = MessageType.GREETINGS;
                output[1] = ANSWER_GREETING;
                break;
            case RESERVATION: // Si c'est une réservation
                int[] dateTime = extractDateTime(message); // On essaye d'extraire les informations de date et heure
                // Les informations de dateTime sont : 0 = infos trouvées (-1 ou 1), 1 = heure, 2 = jour, 3 = mois (de 0 à 11)
                if (dateTime[0] == 1) { // Si on a pu extraire les infos correctement
                    // On vérifie si le créneau est disponible
                    System.out.println(Agenda.getYearOfDayMonth(dateTime[2], dateTime[3]) + "-" + dateTime[3] + "-" + dateTime[2] + "/" + dateTime[1]);
                    Agenda.Availability availability = agenda.isSlotAvailable(Agenda.getYearOfDayMonth(dateTime[2], dateTime[3]) + "-" + dateTime[3] + "-" + dateTime[2], dateTime[1], true);
                    System.out.println(availability);
                    output = new Object[6];
                    output[0] = MessageType.RESERVATION;
                    switch (availability) { // Selon l'état du créneau souhaité, on répond différent message
                        case ALREADY_BOOKED:
                            output[1] = ANSWER_RESERVATION_ERRORS[2];
                            break;
                        case PAST:
                            output[1] = ANSWER_RESERVATION_ERRORS[1];
                            break;
                        case TOO_FAR:
                            output[1] = ANSWER_RESERVATION_ERRORS[0];
                            break;
                        default: // Si le créneau est disponible
                            output[1] = ANSWER_RESERVATION.replace("%day%", Integer.toString(dateTime[2])).replace("%month%", MONTHS[dateTime[3]]).replace("%hourStart%", Integer.toString(dateTime[1])).replace("%hourEnd%", Integer.toString(dateTime[1] + 2));
                    }
                    // On écrit les informations de date et heure et on indique que ça a marché (output[2])
                    output[2] = true;
                    output[3] = dateTime[1];
                    output[4] = dateTime[2];
                    output[5] = dateTime[3];
                } else { // Si on n'a pas pu les extraire, on prévient l'utilisateur
                    output = new Object[3];
                    output[0] = MessageType.RESERVATION;
                    output[1] = ANSWER_RESERVATION_MISSING;
                    output[2] = false;
                }
                break;
            case CONFIRMATION: // Si le message est un message de confirmation (et que le précédent était donc de réservation)
                // On essaye de réserver le créneau, et on sauvegarde l'état
                boolean booked = agenda.bookSlot(user, reservationDateTime[0], Integer.parseInt(reservationDateTime[1]), true);
                output = new Object[3];
                output[0] = MessageType.CONFIRMATION;
                // On dit à l'utilisateur si ça a marché ou non, et on l'indique dans output[2]
                if (booked) {
                    output[1] = ANSWER_CONFIRMATION;
                    output[2] = true;
                } else {
                    output[1] = ANSWER_CONFIRMATION_FAILED;
                    output[2] = false;
                }
                break;
            case NEGATION: // Si l'utilisateur annule
                output = new Object[2];
                output[0] = MessageType.NEGATION;
                output[1] = ANSWER_NEGATION;
                break;
            case INFORMATION: // Si il demande des informations, on récupère tous les créneaux et on les affiche
                String information = "";
                ArrayList<Agenda.Slot> slots = agenda.getAllBookedSlots();
                for (int i = 0; i < slots.size(); i++) {
                    // Les caractères "// " indique un retour à la ligne
                    information += slots.get(i).getSlotString() + (slots.get(i).getUser().equals(user) ? " par vous" : "") + " ;// ";
                }
                output = new Object[2];
                output[0] = MessageType.INFORMATION;
                output[1] = ANSWER_INFORMATION.replace("%info%", information);
                break;
            default: // Si le message n'a pas été reconnu, on répond qu'on n'a pas compris
                output = new Object[2];
                output[0] = MessageType.OTHER;
                output[1] = ANSWER_OTHER;
        }

        return output;
    }

    // Renvoie le type supposé du message, en effectuant des comparaisons des minimums des distances de Levenshtein
    private static MessageType getMessageType(String message) {
        // Pour chaque type, on va vérifier si la distance est inférieure à la précédente. Si oui, on considère ce type comme le bon
        // En cas d'égalité, l'ordre de priorité est : RESERVATION > INFORMATION > CONFIRMATION > NEGATION > SALUTATIONS > AUTRES
        int minDistance = minimumDistanceFromWordToList(message, WLIST_RESERVATION);
        int distance;
        MessageType outputType = MessageType.RESERVATION;

        distance = minimumDistanceFromWordToList(message, WLIST_INFORMATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = MessageType.INFORMATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_CONFIRMATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = MessageType.CONFIRMATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_NEGATION);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = MessageType.NEGATION;
        }

        distance = minimumDistanceFromWordToList(message, WLIST_GREETINGS);
        if (distance < minDistance) {
            minDistance = distance;
            outputType = MessageType.GREETINGS;
        }

        // Si on est bien en dessous du seuil on considère que ce n'est pas un faux positif
        if (minDistance <= LEVENSHTEIN_THRESHOLD) {
            return outputType;
        } else { // Sinon on renvoie le type par défaut
            return MessageType.OTHER;
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

    // Récupère, si possible, les informations de date et d'heure d'une chaîne
    private static int[] extractDateTime(String message) {
        int[] dateTimeOutput;

        // ***HEURE
        // Permet de récupérer un pattern de type "14h" ou "08 h"
        Pattern hourPattern = Pattern.compile("([01]?[0-9]) ?h");
        Matcher hourMatcher = hourPattern.matcher(message);
        boolean hasHour = hourMatcher.find();
        int hour = -1;
        if (hasHour) { // Si on a trouvé le pattern, on récupère l'heure qui se trouve dans le groupe 1
            hour = Integer.parseInt(hourMatcher.group(1));
        }

        // ***Date
        // Permet de récupérer un pattern de type "12/05" ou "28 décembre"
        Pattern datePattern = Pattern.compile("(([0-3]?[0-9])\\/([01]?[0-9]))|(([0-3]?[0-9]) ?(jan|fev|fév|mar|avr|mai|juin|juil|aout|août|sept|oct|nov|dec|déc))");
        Matcher dateMatcher = datePattern.matcher(message);
        boolean hasDate = dateMatcher.find();
        int day = -1, month = -1;
        if (hasDate) { // Si on a trouvé le pattern, selon duquel il s'agit, on récupère différement le jour et le mois
            if (dateMatcher.group(1) != null) {
                day = Integer.parseInt(dateMatcher.group(2));
                month = Integer.parseInt(dateMatcher.group(3)) - 1;
            } else {
                day = Integer.parseInt(dateMatcher.group(5));
                month = getMonthIndex(dateMatcher.group(6));
            }
        }

        // Si on a trouvé les deux, on les renvoie et on met un 1 dans la première case de la sortie
        if (hasHour && hasDate) {
            dateTimeOutput = new int[4];
            dateTimeOutput[0] = 1;
            // On force l'heure à un nombre pair entre 8h et 16h
            if (hour < 8) {
                hour = 8;
            }
            if (hour > 17) {
                hour = 16;
            }
            dateTimeOutput[1] = hour - (hour % 2);
            dateTimeOutput[2] = day;
            dateTimeOutput[3] = month;
        } else { // Si on a pas trouvé, on l'indique par un -1
            dateTimeOutput = new int[1];
            dateTimeOutput[0] = -1;
        }
        return dateTimeOutput;
    }

    // Permet de renvoyer le bon index du mois selon ses premiers caractères, avec ou sans les accents
    private static int getMonthIndex(String month) {
        switch (month) {
            case "jan":
                return 0;
            case "fev":
            case "fév":
                return 1;
            case "mar":
                return 2;
            case "avr":
                return 3;
            case "mai":
                return 4;
            case "juin":
                return 5;
            case "juil":
                return 6;
            case "aout":
            case "août":
                return 7;
            case "sept":
                return 8;
            case "oct":
                return 9;
            case "nov":
                return 10;
            case "dec":
            case "déc":
                return 11;
        }
        return -1;
    }

}
