package fr.insa.kern.projet.classifying;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// Classe qui permet de gérer une vectorisation de mot pour la classification
public class EmbeddingClassifier {

    // Chemin d'accès aux différents fichiers
    public static final String DIC_PATH = "dic.txt"; //Dictionnaire fourni par M. Samet
    public static final String DIC2_PATH = "dic2.txt"; //Dictionnaire personnel
    public static final String KNN_REFERENCES_PATH = "knn_references.txt";

    private int embedding_size; // Taille des vecteurs
    private final Embedding embedding; // Embedding de notre dictionnaire
    private Map<Classifier.MessageType, ArrayList<Vector>> references; // HashMap de vectorisation des phrases de références

    // Instancie la classe avec le chemin du dictionnaire de vectorisation et du fichier contenant les phrases de référence
    public EmbeddingClassifier(String dicPath, String referencePath) {
        embedding = new Embedding();
        try {
            FileReader reader;
            // On lit le dictionnaire ligne par ligne
            // Il est au format <mot> <a0> <a1> ... <aN>, avec une première ligne contenant le nombre de mot et la taille des vecteurs
            reader = new FileReader(dicPath);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            this.embedding_size = -1;
            while ((line = bufferedReader.readLine()) != null) {
                String[] args = line.split(" "); // On découpe la ligne selon les espaces
                if (line.length() < 100) { // Si on est dans la première ligne, on extrait la taille de vectorisation
                    embedding_size = Integer.parseInt(args[1]);
                } else { // Sinon, on créé un tableau de la bonne taille qu'on remplit avec les valeurs contenues dans la ligne
                    double[] values = new double[embedding_size];
                    for (int i = 0; i < embedding_size; i++) {
                        values[i] = Double.parseDouble(args[i + 1]);
                    }
                    embedding.addWord(args[0].toLowerCase(), new Vector(values)); // On ajoute le mot à la HashMap
                }
            }

            // On lit ensuite les références, ligne par ligne
            // Le format est : <Type>:<Phrase>
            // La variable references va est une HashMap d'ArrayList qui va contenir, pour chaque type de message, une liste des vectorisations associées à ce type
            references = new HashMap<>();
            reader = new FileReader(referencePath);
            bufferedReader = new BufferedReader(reader);
            while ((line = bufferedReader.readLine()) != null) {
                String[] args = line.split(":"); // On découpe pour séparer le type et la phrase
                if (!references.containsKey(Classifier.MessageType.valueOf(args[0]))) { // Si le dictionnaire ne contient pas encore ce type, on l'ajoute
                    references.put(Classifier.MessageType.valueOf(args[0]), new ArrayList<>());
                }
                // On calcule la vectorisation de la phrase et on l'ajoute à la liste associée au type
                references.get(Classifier.MessageType.valueOf(args[0])).add(embedding.getSentenceEmbedding(args[1]));
            }

        } catch (IOException e) {}
    }

    // Renvoie le type du voisin le plus proche du message dans l'espace vectorisé (k-NN à un seul voisin)
    public Classifier.MessageType getMessageTypeByClosestNeighbor(String message) {
        // TreeMap triée par ordre décroissant, qui va permettre de savoir quelles phrases sont les plus similaires (plus la similarité cosinus est grande, plus elles sont similaires)
        Map<Double, Classifier.MessageType> points = new TreeMap<>(Comparator.reverseOrder());
        Vector messageEmbedding = embedding.getSentenceEmbedding(message); // On calcule la vectorisation du message
        for (Map.Entry<Classifier.MessageType, ArrayList<Vector>> reference : references.entrySet()) { // On parcourt tous les types possibles
            for (int i = 0; i < reference.getValue().size(); i++) { // Pour chaque type, on parcourt toutes les vectorisations associées
                double similarity = messageEmbedding.cosineSimilarity(reference.getValue().get(i)); // On calcule la similarité pour toutes ces combinaisons
                points.put(similarity, reference.getKey()); // On ajoute la combinaison similarité/type
            }
        }

        // On renvoie ensuite le plus proche voisin
        Map.Entry<Double, Classifier.MessageType> closestNeighbor = points.entrySet().iterator().next();
        if (closestNeighbor.getKey() > 0) {
            return closestNeighbor.getValue();
        } else {
            return Classifier.MessageType.OTHER;
        }
    }

    // Renvoie le type dont la similarité est la plus grande sur toutes les phrases de référence (k-NN pondéré sur tous les voisins)
    public Classifier.MessageType getMessageTypeByWeighting(String message) {
        // Contient la similarité entre le message et chaque type de référence sous forme de la somme des similarités
        Map<Classifier.MessageType, Double> points = new HashMap<>();

        Vector messageEmbedding = embedding.getSentenceEmbedding(message); // On calcule la vectorisation du message
        for (Map.Entry<Classifier.MessageType, ArrayList<Vector>> reference : references.entrySet()) { // On parcourt tous les types possibles
            for (int i = 0; i < reference.getValue().size(); i++) { // Pour chaque type, on parcourt toutes les vectorisations associées
                double similarity = messageEmbedding.cosineSimilarity(reference.getValue().get(i)); // On calcule la similarité pour toutes ces combinaisons
                if (!points.containsKey(reference.getKey())) { // Si on a pas déjà ajouté le type, on le fait
                    points.put(reference.getKey(), 0.0);
                }
                // On ajoute ensuite la similarité à la somme des similarités de ce type de message
                points.put(reference.getKey(), points.get(reference.getKey()) + similarity);
            }
        }

        // On va ensuite chercher le type de message avec la plus forte similarité (ramenée au nombre de phrases de référence de chque type=
        double maxSimilarity = Double.MIN_VALUE;
        Classifier.MessageType maxSimilarityType = Classifier.MessageType.OTHER;
        for (Map.Entry<Classifier.MessageType, Double> point : points.entrySet()) {
            if (point.getValue() / (double)references.get(point.getKey()).size() > maxSimilarity) {
                maxSimilarity = point.getValue() / (double)references.get(point.getKey()).size();
                maxSimilarityType = point.getKey();
            }
        }

        if (maxSimilarity > 0) {
            return maxSimilarityType;
        } else {
            return Classifier.MessageType.OTHER;
        }
    }

}
