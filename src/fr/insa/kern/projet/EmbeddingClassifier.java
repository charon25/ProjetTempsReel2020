package fr.insa.kern.projet;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// Classe qui permet de gérer une vectorisation de mot pour la classification
public class EmbeddingClassifier {

    // Nombre de points à prendre en compte pour le k-NN
    private final int KNN_RANGE = 3;

    // Chemin d'accès aux différents fichiers
    public static final String DIC_PATH = "dic.txt";
    public static final String KNN_REFERENCES_PATH = "knn_references.txt";

    private int embedding_size; // Taille des vectuers
    private final Map<String, double[]> embeddings; // HashMap des vecteurs de chaque mot
    private Map<Classifier.MessageType, ArrayList<double[]>> references; // HashMap de vectorisation des phrases de références

    // Calcule la somme de deux vectorisations
    private double[] embeddingsSum(double[] embed1, double[] embed2) {
        double result[] = new double[embed1.length];
        Arrays.setAll(result, i -> embed1[i] + embed2[i]); // Lambda expression qui additionne terme à terme
        return result;
    }
    // Calcule le module d'un vecteur
    private double embeddingMagnitude(double[] embed) {
        double result = 0.0;
        for (int i = 0; i < embed.length; i++) { // On parcourt le vecteur en faisant la somme des carrés des termes
            result += embed[i] * embed[i];
        }
        return Math.sqrt(result); // On prend la racine carrée du résultat
    }
    // Calcule la similarité cosinus entre deux vecteurs. Elle va de -1 à 1, 1 : vecteurs identiques, -1 : vecteurs opposés
    // Elle vaut le produit scalaire des deux vecteurs divisés par le produit de leur module
    private double cosineSimilarity(double[] embed1, double[] embed2) {
        double magnitudes = embeddingMagnitude(embed1) * embeddingMagnitude(embed2); // On calcule le produit de leur module
        if (magnitudes == 0) { // Si le produit vaut 0 (donc au moins un des deux vecteurs est nul), on renvoie 0 pour éviter la division
            return 0.0;
        }
        double result = 0.0;
        for (int i = 0; i < embed1.length; i++) { // On parcourt les deux vecteurs en faisant le produit vectoriel
            result += (embed1[i] * embed2[i]) / magnitudes;
        }
        return result;
    }

    // Instancie la classe avec le chemin du dictionnaire de vectorisation et du fichier contenant les phrases de référence
    public EmbeddingClassifier(String dicPath, String referencePath) {
        embeddings = new HashMap<>();
        try {
            FileReader reader = null;
            // On lit le dictionnaire ligne par ligne
            // Il est au format <mot> <a0> <a1> ... <aN>, avec une première ligne contenant le nombre de mot et la taille des vecteurs
            reader = new FileReader(dicPath);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            this.embedding_size = -1;
            while ((line = bufferedReader.readLine()) != null) {
                String args[] = line.split(" "); // On découpe la ligne selon les espaces
                if (line.length() < 100) { // Si on est dans la première ligne, on extrait la taille de vectorisation
                    embedding_size = Integer.parseInt(args[1]);
                } else { // Sinon, on créé un tableau de la bonne taille qu'on remplit avec les valeurs contenues dans la ligne
                    double values[] = new double[embedding_size];
                    for (int i = 0; i < embedding_size; i++) {
                        values[i] = Double.parseDouble(args[i + 1]);
                    }
                    embeddings.put(args[0].toLowerCase(), values); // On ajoute le mot à la HashMap
                }
            }

            // On lit ensuite les références, ligne par ligne
            // Le format est : <Type>:<Phrase>
            // La variable references va est une HashMap d'ArrayList qui va contenir, pour chaque type de message, une liste des vectorisations associées à ce type
            references = new HashMap<>();
            reader = new FileReader(referencePath);
            bufferedReader = new BufferedReader(reader);
            while ((line = bufferedReader.readLine()) != null) {
                String args[] = line.split(":"); // On découpe pour séparer le type et la phrase
                if (!references.containsKey(Classifier.MessageType.valueOf(args[0]))) { // Si le dictionnaire ne contient pas encore ce type, on l'ajoute
                    references.put(Classifier.MessageType.valueOf(args[0]), new ArrayList<>());
                }
                // On calcule la vectorisation de la phrase et on l'ajoute à la liste associée au type
                if (args[1].equals("Salut")) {
                    System.out.println(getSentenceEmbedding(args[1])[0]);
                }
                references.get(Classifier.MessageType.valueOf(args[0])).add(getSentenceEmbedding(args[1]));
            }

        } catch (IOException e) {}
    }

    // Renvoie la vectorisation d'un mot. S'il n'est pas dans la liste, on renvoie un vecteur nul
    private double[] getWordEmbedding(String word) {
        if (embeddings.containsKey(word)) {
            return embeddings.get(word);
        } else {
            return new double[embedding_size];
        }
    }
    // Calcule la vectorisation d'une phrase comme la moyenne des vectorisations des mots qui la constitue
    private double[] getSentenceEmbedding(String sentence) {
        System.out.println(sentence);
        sentence = Classifier.formatMessage(sentence); // On formate la phrase pour enlever les prépositions, les majuscules, la ponctuation
        System.out.println(sentence);
        int word_count = sentence.split(" ").length; // On compte combien on a de mots dedans
        double result[] = new double[embedding_size];
        for (String word : sentence.split(" ")) { // On parcourt tous les mots et on fait la somme de leur vectorisation
            result = embeddingsSum(result, getWordEmbedding(word));
        }
        for (int i = 0; i < embedding_size; i++) { // On divise par le nombre de mots de la phrase pour normaliser
            result[i] /= word_count;
        }
        return result;
    }

    // Renvoie le type du voisin le plus proche du message dans l'espace vectorisé (k-NN à un seul voisin)
    public Classifier.MessageType getMessageTypeByClosestNeighbor(String message) {
        // TreeMap triée par ordre décroissant, qui va permettre de savoir quelles phrases sont les plus similaires (plus la similarité cosinus est grande, plus elles sont similaires)
        Map<Double, Classifier.MessageType> points = new TreeMap<>(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        });
        double[] messageEmbedding = getSentenceEmbedding(message); // On calcule la vectorisation du message
        int knn_offset = 0;
        for (Map.Entry<Classifier.MessageType, ArrayList<double[]>> reference : references.entrySet()) { // On parcourt tous les types possibles
            for (int i = 0; i < reference.getValue().size(); i++) { // Pour chaque type, on parcourt toutes les vectorisations associées
                double similarity = cosineSimilarity(messageEmbedding, reference.getValue().get(i)); // On calcule la similarité pour toutes ces combinaisons
                while (points.containsKey(similarity)) { // Comme on trie selon la clé, et qu'elle doit être unique, si elle est déjà dedans, on ajoute une petite valeur pour la différencier
                    // Mais on va également prendre en compte cette égalité en ajoutant 1 au nombre de voisins pris en compte pour le knn
                    similarity += 0.00000001;
                }
                points.put(similarity, reference.getKey()); // On ajoute la combinaison similarité/type
            }
        }

        // On renvoie ensuite le plus proche voisin
        return points.entrySet().iterator().next().getValue();
    }

    // Renvoie le type dont la similarité est la plus grande sur toutes les phrases de référence
    public Classifier.MessageType getMessageTypeByWeighting(String message) {
        // Contient la similarité entre le message et chaque type de référence sous forme de la somme des similarités
        Map<Classifier.MessageType, Double> points = new HashMap<>();

        double[] messageEmbedding = getSentenceEmbedding(message); // On calcule la vectorisation du message
        int knn_offset = 0;
        for (Map.Entry<Classifier.MessageType, ArrayList<double[]>> reference : references.entrySet()) { // On parcourt tous les types possibles
            for (int i = 0; i < reference.getValue().size(); i++) { // Pour chaque type, on parcourt toutes les vectorisations associées
                double similarity = cosineSimilarity(messageEmbedding, reference.getValue().get(i)); // On calcule la similarité pour toutes ces combinaisons
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
        return maxSimilarityType;
    }

}
