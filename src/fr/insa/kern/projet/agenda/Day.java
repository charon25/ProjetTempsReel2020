package fr.insa.kern.projet.agenda;

import fr.insa.kern.projet.classifying.Classifier;

import java.util.Calendar;

// Classe qui permet de gérer un jour
public class Day {

    private final int SLOT_COUNT = 5; // Nombre de créneau par jour, fixé
    private Calendar date; // Date du jour
    private Slot[] slots; // Liste des créneaux. L'index i est entre 0 et 4, et l'heure h entre 8 et 16 par pas de 2
    // il faut donc appliquer i = (h - 8) / 2 ou h = 2 * i + 8 selon les cas

    // Constructeur à partir d'une date de calendrier
    public Day(Calendar dayDate) {
        this.date = Calendar.getInstance();
        this.date.setTimeInMillis(dayDate.getTimeInMillis()); // On crée la date et on lui donne le bon temps
        this.slots = new Slot[SLOT_COUNT];
    }

    // Constructeur à partir d'un string, lorsque chargé depuis un fichier
    // Le format est : aaaa-mm-jj[:h/<user>/<date de réservation>, ...]
    public Day(String dayString) {
        try { // Au cas où il se passe mal quelque chose
            this.date = Calendar.getInstance();
            // On coupe selon ":" et puis selon "-" pour récupérer les morceaux de la date
            String[] args = dayString.split(":")[0].split("-");
            this.date.set(Calendar.YEAR, Integer.parseInt(args[0]));
            this.date.set(Calendar.MONTH, Integer.parseInt(args[1]));
            this.date.set(Calendar.DATE, Integer.parseInt(args[2]));
            this.date.set(Calendar.HOUR, 0);
            this.date.set(Calendar.MINUTE, 0);
            this.date.set(Calendar.SECOND, 0);
            this.date.set(Calendar.MILLISECOND, 0);

            this.slots = new Slot[SLOT_COUNT];
            if (dayString.contains(":")) { // On vérifie qu'il y a des créneaux enregistrés, si non on ne fait rien
                String[] loadedSlots = dayString.split(":")[1].split(","); // On coupe selon ",", qui sépare les créneaux
                for (String loadedSlot : loadedSlots) {
                    // On récupère les différents morceaux, dans l'ordre : heure, utilisateur, date de la réservation
                    args = loadedSlot.split("/");
                    this.slots[Integer.parseInt(args[0])] = new Slot(args[1], Integer.parseInt(args[0]), Long.parseLong(args[2]), this.date);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {}
    }

    // Réserve un créneau
    // user : utilisateur, hour : heure du créneau
    public boolean bookSlot(String user, int hour) {
        hour = getIndexFromHour(hour); // On récupère l'index
        if (slots[hour] == null) { // Si le créneau est libre on le réserve, sinon on renvoie false pour indiquer un problème
            slots[hour] = new Slot(user, date, hour);
            return true;
        } else {
            return false;
        }
    }

    // Renvoie l'index selon l'heure
    private int getIndexFromHour(int hour) {
        return ((hour - 8) / 2);
    }

    // Renvoie la date de l'instance
    public Calendar getDate() {
        return date;
    }

    // Renvoie la date au format aaaa-mm-jj
    public String getDateString() {
        return date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH) + "-" + date.get(Calendar.DATE);
    }

    // Renvoie la date au format jj/mm/aaaa
    public String getDateStringDaysFirst() {
        return date.get(Calendar.DATE) + " " + Classifier.MONTHS[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR);
    }

    // Renvoie la liste des créneaux
    public Slot[] getSlots() {
        return slots;
    }

    // Renvoie le créneau correspondant à l'heure demandée
    public Slot getSlotByHour(int hour) {
        return slots[getIndexFromHour(hour)];
    }

    // Renvoie le jour au format correct pour l'enregistrement
    // Format : aaaa-mm-jj[:h/<user>/<date de réservation>, ...]
    public String toString() {
        String output = getDateString() + ":";
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] != null) {
                output += slots[i].toString() + ",";
            }
        }
        return output.substring(0, output.length() - 1);
    }
}
