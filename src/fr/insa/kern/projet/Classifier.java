package fr.insa.kern.projet;

import java.util.ArrayList;
import java.util.regex.*;

public class Classifier {

    public enum MessageType {GREETINGS, RESERVATION, CONFIRMATION, NEGATION, INFORMATION, OTHER}

    private static final int LEVENSHTEIN_THRESHOLD = 2;
    private static final String[] WORDS_TO_REMOVE = {"le", "la", "les", "un", "une", "des", "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "nos", "votre", "vos", "leur", "leurs", "à", "y", "du", "de", "ou", "où", "en", "au", "dès", "par", "sur", "sûr", "vers", "pour", "sans"};
    private static final String[] PUNCTATION = {"?", "!", ".", ",", ";", "=", "'", "\"", ":"};
    public static final String[] MONTHS = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

    private static final String[] WLIST_GREETINGS = {"salut", "bonjour", "hello", "coucou", "bonsoir"};
    private static final String[] WLIST_RESERVATION = {"réserver", "reserver", "réservation", "reservation", "creneau", "créneau", "reserve", "réserve", "reserves", "réserves"};
    private static final String[] WLIST_CONFIRMATION = {"oui", "yes", "tout à fait", "tout a fait", "exactement", "exact", "parfait", "d'acc", "absolument"};
    private static final String[] WLIST_NEGATION = {"non", "pas du tout", "faux"};
    private static final String[] WLIST_INFORMATION = {"information", "informations", "info", "infos", "renseignement", "renseignements", "savoir", "liste", "deja", "déjà"};

    private static final String ANSWER_GREETING = "Bonjour.";
    private static final String ANSWER_RESERVATION = "Créneau souhaité : %hourStart%h-%hourEnd%h le %day% %month% . Est-ce correct ?";
    private static final String[] ANSWER_RESERVATION_ERRORS = {"Le créneau souhaité est au-delà de la date limite du calendrier. Veuillez en choisir un autre.", "Le créneau souhaité est dans le passé. Veuillez en choisir un autre.", "Le créneau souhaité est déjà réservé. Veuillez en choisir un autre."};
    private static final String ANSWER_RESERVATION_MISSING = "Impossible de récupérer l'heure ou la date du rendez-vous souhaité. Merci de les renvoyer.";
    private static final String ANSWER_CONFIRMATION_FAILED = "Une erreur est survenue. Merci de vérfier que le créneau est libre.";
    private static final String ANSWER_CONFIRMATION = "Créneau correctement réservé.";
    private static final String ANSWER_NEGATION = "Demande annulée.";
    private static final String ANSWER_INFORMATION = "Les créneaux déjà occupés sont : //%info%";
    private static final String ANSWER_OTHER = "Impossible de comprendre le message. Merci de le reformuler et de le renvoyer.";

    //user : utilisateur qui fait la demande
    //message : message de cet utilisateur
    //agenda : instance de l'agenda conservée par le serveur
    //reservationDateTime : tableau de string format {"yyyy-mm-dd", h} (mois de 0 à 11)
    //previousIsReservation : true si la demande précédente était un rdv
    public static Object[] getResponse(String user, String message, Agenda agenda, String[] reservationDateTime, boolean previousIsReservation) {
        Object[] output;
        for (String word : WORDS_TO_REMOVE) {
            message = message.replace(" " + word + " ", " ");
        }
        for (String punctuation : PUNCTATION) {
            message = message.replace(punctuation, "");
        }
        message = message.replaceAll(" +", " ");
        MessageType type = getMessageType(message);
        if (previousIsReservation && extractDateTime(message)[0] == 1) {
            type = MessageType.RESERVATION;
        }
        if (!previousIsReservation && (type == MessageType.NEGATION || type == MessageType.CONFIRMATION)) {
            type = MessageType.OTHER;
        }
        switch (type) {
            case GREETINGS:
                output = new Object[2];
                output[0] = MessageType.GREETINGS;
                output[1] = ANSWER_GREETING;
                break;
            case RESERVATION:
                int[] dateTime = extractDateTime(message);
                //0 : infos trouvées, 1 : heure, 2 : jour, 3 : mois (de 0 à 11)
                if (dateTime[0] == 1) {
                    Agenda.Availability availability = agenda.isSlotAvailable(Agenda.getYearOfDayMonth(dateTime[2], dateTime[3]) + "-" + dateTime[3] + "-" + dateTime[2], dateTime[1], true);
                    output = new Object[6];
                    output[0] = MessageType.RESERVATION;
                    switch (availability) {
                        case ALREADY_BOOKED:
                            output[1] = ANSWER_RESERVATION_ERRORS[2];
                            break;
                        case PAST:
                            output[1] = ANSWER_RESERVATION_ERRORS[1];
                            break;
                        case TOO_FAR:
                            output[1] = ANSWER_RESERVATION_ERRORS[0];
                            break;
                        default:
                            output[1] = ANSWER_RESERVATION.replace("%day%", Integer.toString(dateTime[2])).replace("%month%", MONTHS[dateTime[3]]).replace("%hourStart%", Integer.toString(dateTime[1])).replace("%hourEnd%", Integer.toString(dateTime[1] + 2));
                    }
                    output[2] = true;
                    output[3] = dateTime[1];
                    output[4] = dateTime[2];
                    output[5] = dateTime[3];
                } else {
                    output = new Object[3];
                    output[0] = MessageType.RESERVATION;
                    output[1] = ANSWER_RESERVATION_MISSING;
                    output[2] = false;
                }
                break;
            case CONFIRMATION:
                boolean booked = agenda.bookSlot(user, reservationDateTime[0], Integer.parseInt(reservationDateTime[1]), true);
                output = new Object[3];
                output[0] = MessageType.CONFIRMATION;
                if (booked) {
                    output[1] = ANSWER_CONFIRMATION;
                    output[2] = true;
                } else {
                    output[1] = ANSWER_CONFIRMATION_FAILED;
                    output[2] = false;
                }
                break;
            case NEGATION:
                output = new Object[2];
                output[0] = MessageType.NEGATION;
                output[1] = ANSWER_NEGATION;
                break;
            case INFORMATION:
                String information = "";
                ArrayList<Agenda.Slot> slots = agenda.getAllSlots();
                for (int i = 0; i < slots.size(); i++) {
                    information += slots.get(i).getReservationString() + (slots.get(i).getUser().equals(user) ? " par vous" : "") + " ;//";
                }
                output = new Object[2];
                output[0] = MessageType.INFORMATION;
                output[1] = ANSWER_INFORMATION.replace("%info%", information);
                break;
            default:
                output = new Object[2];
                output[0] = MessageType.OTHER;
                output[1] = ANSWER_OTHER;
        }

        return output;
    }

    private static MessageType getMessageType(String message) {
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

        if (minDistance <= LEVENSHTEIN_THRESHOLD) {
            return outputType;
        } else {
            return MessageType.OTHER;
        }
    }

    private static int minimumDistanceFromWordToList(String message, String[] words) {
        int minimum = Integer.MAX_VALUE;
        int distance;
        for (String ref_word : words) {
            for (String mes_word : message.split(" ")) {
                distance = levenshteinDistance(ref_word.toLowerCase(), mes_word.toLowerCase());
                //System.out.println(ref_word + "/" + mes_word + " : " + levenshteinDistance(ref_word.toLowerCase(), mes_word.toLowerCase()));
                if (distance <= minimum) {
                    minimum = distance;
                }
            }
        }
        return minimum;
    }

    //Permet de connaître le nombre de modification à faire dans un mot pour obtenir l'autre
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
                //d[i - 1][j] + 1 : suppression du caractère de word1
                //d[i][j - 1] + 1 : insertion du caractère de word2 dans word1
                //d[i - 1][j - 1] + cout : substitution des deux caractères
                d[i][j] = Math.min(d[i - 1][j] + 1, Math.min(d[i][j - 1] + 1, d[i - 1][j - 1] + cout));
            }
        }
        return d[word1.length()][word2.length()];
    }

    private static int[] extractDateTime(String message) {
        int[] dateTimeOutput;

        //***HEURE
        //Permet de récupérer un pattern de type "14h" ou "08 h"
        Pattern hourPattern = Pattern.compile("([01]?[0-9]) ?h");
        Matcher hourMatcher = hourPattern.matcher(message);
        boolean hasHour = hourMatcher.find();
        int hour = -1;
        if (hasHour) {
            hour = Integer.parseInt(hourMatcher.group(1));
        }

        //***Date
        //Permet de récupérer un pattern de type "12/05" ou "28 décembre"
        Pattern datePattern = Pattern.compile("(([0-3]?[0-9])\\/([01]?[0-9]))|(([0-3]?[0-9]) ?(jan|fev|fév|mar|avr|mai|juin|juil|aout|août|sept|oct|nov|dec|déc))");
        Matcher dateMatcher = datePattern.matcher(message);
        boolean hasDate = dateMatcher.find();
        int day = -1, month = -1;
        if (hasDate) {
            if (dateMatcher.group(1) != null) {
                day = Integer.parseInt(dateMatcher.group(2));
                month = Integer.parseInt(dateMatcher.group(3)) - 1;
            } else {
                day = Integer.parseInt(dateMatcher.group(5));
                month = getMonthIndex(dateMatcher.group(6));
            }
        }

        if (hasHour && hasDate) {
            dateTimeOutput = new int[4];
            dateTimeOutput[0] = 1;
            if (hour < 8) {
                hour = 8;
            }
            if (hour > 17) {
                hour = 16;
            }
            dateTimeOutput[1] = hour - (hour % 2);
            dateTimeOutput[2] = day;
            dateTimeOutput[3] = month;
        } else {
            dateTimeOutput = new int[1];
            dateTimeOutput[0] = -1;
        }
        return dateTimeOutput;
    }

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
