package fr.insa.kern.projet.classifying;

import fr.insa.kern.projet.agenda.*;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Classifier {

    // Enumeration qui permet de classer les messages reçus
    public enum MessageType {GREETINGS, RESERVATION, CONFIRMATION, NEGATION, INFORMATION, OTHER}

    // Mots à supprimer, comme les prépositions et déterminants
    private static final String[] WORDS_TO_REMOVE = {"je", "j", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "c", "ce", "cette", "ces", "le", "la", "les", "un", "une", "des", "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "nos", "votre", "vos", "leur", "leurs", "à", "y", "du", "de", "ou", "où", "en", "au", "dès", "par", "sur", "sûr", "vers", "pour", "sans"};
    // Symboles de ponctuation à retirer
    private static final String[] PUNCTATION = {"?", "!", ".", ",", ";", "=", "'", "\"", ":"};
    // Mois en toute lettre (de 0 à 11)
    public static final String[] MONTHS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

    // REPONSE DU CHATBOT SELON LE CAS (les %nom% sont remplacés dans le code)
    private static final String ANSWER_GREETING = "Bonjour.";
    private static final String ANSWER_RESERVATION = "Créneau souhaité : %hourStart%h-%hourEnd%h le %day% %month%. Est-ce correct ?";
    private static final String[] ANSWER_RESERVATION_ERRORS = {"Le créneau souhaité est au-delà de la date limite du calendrier. Veuillez en choisir un autre.", "Le créneau souhaité est dans le passé. Veuillez en choisir un autre.", "Le créneau souhaité est déjà réservé. Veuillez en choisir un autre.", "Vous avez déjà réservé ce créneau."};
    private static final String ANSWER_RESERVATION_MISSING = "Impossible de récupérer l'heure ou la date du rendez-vous souhaité. Merci de les renvoyer.";
    private static final String ANSWER_CONFIRMATION_FAILED = "Une erreur est survenue. Merci de vérifier que le créneau est libre.";
    private static final String ANSWER_CONFIRMATION = "Créneau correctement réservé.";
    private static final String ANSWER_NEGATION = "Demande annulée.";
    private static final String ANSWER_INFORMATION = "Réservation possible tous les jours jusqu'au %maxdate% de 8h à 16h par créneau de 2h. //Les créneaux déjà occupés sont : //%info%";
    private static final String ANSWER_INFORMATION_EMPTY = "Réservation possible tous les jours jusqu'au %maxdate% de 8h à 16h par créneau de 2h. //Tous les créneaux sont disponibles.";
    private static final String ANSWER_OTHER = "Impossible de comprendre le message. Merci de le reformuler et de le renvoyer.";

    // Renvoie la réponse dans un tableau d'objet qui contient les infos suivantes (taille minimum 2, optionnelle jusqu'à 6) :
    // 0 : type du message (MessageType)
    // 1 : réponse à renvoyer (String)
    // 2 : indique un bon fonctionnement (boolean) [op]
    // 3 : heure de la réservation (int) [op]
    // 4 : jour de la réservation (int) [op]
    // 5 : mois de la réservation (int) [op]
    // 6 : réservation possible (boolean) [op]
    // PARAMETRES :
    // getMessageType : fonction à passer en paramètres qui s'occupe de la classification du message dans les différents types. Elle prend un String et renvoie un MessageType
    // user : utilisateur qui fait la demande
    // message : message de cet utilisateur
    // agenda : instance de l'agenda conservée par le serveur
    // reservationDateTime : tableau de string format {"yyyy-mm-dd", h} (mois de 0 à 11)
    // previousIsReservation : true si la demande précédente était un rdv
    public static Object[] getResponse(Function<String, MessageType> getMessageType, String user, String message, Agenda agenda, String[] reservationDateTime, boolean previousIsReservation) {
        // On met en forme le message
        message = formatMessage(message);

        // On récupère le type de message
        MessageType type = getMessageType.apply(message);

        // Si le précédent message était un de réservation et que le message actuel contient des informations de date et heure
        // alors on considère que ce message aussi est de réservation
        // Utilisé dans le cas où le message précédent demandait une réservation sans préciser de date et d'heure
        if (previousIsReservation && extractDateTime(message)[0] == 1) {
            type = MessageType.RESERVATION;
        }

        // Si le message précédent n'était pas de réservation, alors les message d'approbation et de négation sont considérés comme du bruit
        if (!previousIsReservation && (type == MessageType.NEGATION || type == MessageType.CONFIRMATION)) {
            type = MessageType.OTHER;
        }

        switch (type) { // Selon le type du message
            case GREETINGS: // Si ce sont des salutations, on en renvoie
                return greetingsResponse();
            case RESERVATION: // Si c'est une réservation
                return reservationReponse(agenda, message, user);
            case CONFIRMATION: // Si le message est un message de confirmation (et que le précédent était donc de réservation)
                return confirmationResponse(agenda, user, reservationDateTime);
            case NEGATION: // Si l'utilisateur annule
                return negationResponse();
            case INFORMATION: // Si il demande des informations, on récupère tous les créneaux et on les affiche
                return informationResponse(agenda, user);
            default: // Si le message n'a pas été reconnu, on répond qu'on n'a pas compris
                return defaultResponse();
        }
    }

    // Les fonctions ci-dessous renvoient les informations nécessaires au serveur selon le type du message
    private static Object[] greetingsResponse() {
        Object[] output = new Object[2];
        output[0] = MessageType.GREETINGS;
        output[1] = ANSWER_GREETING;
        return output;
    }
    private static Object[] reservationReponse(Agenda agenda, String message, String user) {
        int[] dateTime = extractDateTime(message); // On essaye d'extraire les informations de date et heure
        // Les informations de dateTime sont, par index : 0 = infos trouvées (-1 ou 1), 1 = heure, 2 = jour, 3 = mois (de 0 à 11)
        if (dateTime[0] == 1) { // Si on a pu extraire les infos correctement
            // On vérifie si le créneau est disponible
            Agenda.Availability availability = agenda.isSlotAvailable(Agenda.getYearOfDayMonth(dateTime[2], dateTime[3]) + "-" + dateTime[3] + "-" + dateTime[2], dateTime[1], true, user);
            Object[] output = new Object[7];
            output[0] = MessageType.RESERVATION;
            switch (availability) { // Selon l'état du créneau souhaité, on répond différent message
                case ALREADY_BOOKED_BY_YOU:
                    output[1] = ANSWER_RESERVATION_ERRORS[3];
                    output[6] = false;
                    break;
                case ALREADY_BOOKED:
                    output[1] = ANSWER_RESERVATION_ERRORS[2];
                    output[6] = false;
                    break;
                case PAST:
                    output[1] = ANSWER_RESERVATION_ERRORS[1];
                    output[6] = false;
                    break;
                case TOO_FAR:
                    output[1] = ANSWER_RESERVATION_ERRORS[0];
                    output[6] = false;
                    break;
                default: // Si le créneau est disponible
                    output[1] = ANSWER_RESERVATION.replace("%day%", Integer.toString(dateTime[2])).replace("%month%", MONTHS[dateTime[3]]).replace("%hourStart%", Integer.toString(dateTime[1])).replace("%hourEnd%", Integer.toString(dateTime[1] + 2));
                    output[6] = true;
            }
            // On écrit les informations de date et heure et on indique que ça a marché (output[2])
            output[2] = true;
            output[3] = dateTime[1];
            output[4] = dateTime[2];
            output[5] = dateTime[3];
            return output;
        } else { // Si on n'a pas pu les extraire, on prévient l'utilisateur
            Object[] output = new Object[3];
            output[0] = MessageType.RESERVATION;
            output[1] = ANSWER_RESERVATION_MISSING;
            output[2] = false;
            return output;
        }
    }
    private static Object[] confirmationResponse(Agenda agenda, String user, String[] reservationDateTime) {
        // On essaye de réserver le créneau, et on sauvegarde l'état
        boolean booked = agenda.bookSlot(user, reservationDateTime[0], Integer.parseInt(reservationDateTime[1]), true);
        Object[] output = new Object[3];
        output[0] = MessageType.CONFIRMATION;
        // On dit à l'utilisateur si ça a marché ou non, et on l'indique dans output[2]
        if (booked) {
            output[1] = ANSWER_CONFIRMATION;
            output[2] = true;
        } else {
            output[1] = ANSWER_CONFIRMATION_FAILED;
            output[2] = false;
        }
        return output;
    }
    private static Object[] negationResponse() {
        Object[] output = new Object[2];
        output[0] = MessageType.NEGATION;
        output[1] = ANSWER_NEGATION;
        return output;
    }
    private static Object[] informationResponse(Agenda agenda, String user) {
        String information = "";
        ArrayList<Slot> slots = agenda.getAllBookedSlots();
        String lastAvailableDay = agenda.getLastAvailableDay();
        if (slots.size() == 0) {
            Object[] output = new Object[2];
            output[0] = MessageType.INFORMATION;
            output[1] = ANSWER_INFORMATION_EMPTY.replace("%maxdate%", lastAvailableDay);
            return output;
        } else {
            for (Slot slot : slots) {
                // Les caractères "// " indique un retour à la ligne
                information += slot.getSlotString() + (slot.getUser().equals(user) ? " par vous" : "") + " ;//";
            }
            Object[] output = new Object[2];
            output[0] = MessageType.INFORMATION;
            output[1] = ANSWER_INFORMATION.replace("%info%", information).replace("%maxdate%", lastAvailableDay);
            return output;
        }
    }
    private static Object[] defaultResponse() {
        Object[] output = new Object[2];
        output[0] = MessageType.OTHER;
        output[1] = ANSWER_OTHER;
        return output;
    }

    // Renvoie le message formaté : on enlève la ponctuation et les mots à supprimer (prépositions, déterminants, ...)
    public static String formatMessage(String message) {
        // On met le message en minuscule
        message = message.toLowerCase();
        // On supprime tous les signes de ponctuation
        for (String punctuation : PUNCTATION) {
            message = message.replace(punctuation, "");
        }
        // Pour chaque mot à supprimer, on l'enlève qu'il soit au milieu, au début ou à la fin
        for (String word : WORDS_TO_REMOVE) {
            message = message.replace(" " + word + " ", " "); // Au milieu
            if (message.startsWith(word + " ")) { // Au début
                message = message.substring(word.length());
            }
            if (message.endsWith(" " + word)) { // A la fin
                message = message.substring(0, message.length() - word.length());
            }
        }
        // Enfin on remplace les espaces multiples par une seule, et on enlève les espaces en début et fin de message
        message = message.replaceAll(" +", " ").replaceAll("^ ", "").replaceAll(" $", "");

        return message;
    }

    // Récupère, si possible, les informations de date et d'heure d'une chaîne
    private static int[] extractDateTime(String message) {
        int[] dateTimeOutput;

        // ***HEURE
        // Permet de récupérer un pattern de type "14h" ou "14 h" ou "08 h" ou "8h"
        Pattern hourPattern = Pattern.compile("([01]?[0-9]) ?h");
        Matcher hourMatcher = hourPattern.matcher(message);
        boolean hasHour = hourMatcher.find();
        int hour = -1;
        if (hasHour) { // Si on a trouvé le pattern, on récupère l'heure qui se trouve dans le groupe 1
            hour = Integer.parseInt(hourMatcher.group(1));
        }

        // ***Date
        // Permet de récupérer un pattern de type "12/05" ou "28 décembre"
        Pattern datePattern = Pattern.compile("(([0-3]?[0-9])/([01]?[0-9]))|(([0-3]?[0-9]) ?(jan|fev|fév|mar|avr|mai|juin|juil|aout|août|sept|oct|nov|dec|déc))");
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
