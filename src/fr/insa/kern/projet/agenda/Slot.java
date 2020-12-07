package fr.insa.kern.projet.agenda;

import fr.insa.kern.projet.classifying.Classifier;

import java.util.Calendar;

// Classe qui permet de gérer un créneau
public class Slot {

    private final String user; // Utilisateur ayant réservé le créneau
    private final Calendar day; // Jour du créneau
    private final Calendar reservationDate; // Date de la réservation
    private final int hour; // Heure du créneau

    // Instancie un créneau avec un utilisateur, un jour et une heure
    public Slot(String user, Calendar day, int hour) {
        this.hour = hour;
        this.user = user;
        this.day = day;
        this.reservationDate = Calendar.getInstance(); // On récupère la date actuelle
    }

    // Instancie un créneau avec une heure de réservation déjà connue, lorsqu'on charge depuis un fichier
    public Slot(String user, int hour, long reservationTime, Calendar day) {
        this.hour = hour;
        this.user = user;
        this.day = day;
        this.reservationDate = Calendar.getInstance();
        this.reservationDate.setTimeInMillis(reservationTime);
    }

    // ACCESSEURS
    public String getUser() {
        return user;
    }
    public Calendar getReservationDate() {
        return reservationDate;
    }

    // Renvoie le créneau sous la forme :
    // <heure début 2 chiffres>h-<heure fin 2 chiffres>h le jj <mois en lettre>
    public String getSlotString() {
        return padLeft(Integer.toString(getHour()), 2) + "h-" + padLeft(Integer.toString(getHour() + 2), 2) + "h le " + day.get(Calendar.DATE) + " " + Classifier.MONTHS[day.get(Calendar.MONTH)];
    }

    // Renvoie l'heure réelle du créneau
    public int getHour() {
        return 2 * hour + 8;
    }

    // Renvoie la version String du créneau pour l'enregistrement dans un fichier
    public String toString() {
        return hour + "/" + user + "/" + reservationDate.getTimeInMillis();
    }

    // Ajoute des "0" à gauche du nombre pour atteindre la longueur demandée
    private String padLeft(String str, int length) {
        if (str.length() >= length) {
            return str;
        } else {
            for (int i = length; i > str.length(); i--) {
                str = "0" + str;
            }
        }
        return str;
    }
}