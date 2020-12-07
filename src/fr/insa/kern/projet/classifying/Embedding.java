package fr.insa.kern.projet.classifying;

import java.util.HashMap;
import java.util.Map;

// Encapsule le dictionnaire de vecteur
public class Embedding {

    private final Map<String, Vector> vectors;
    private int embedding_size;

    public Map<String, Vector> getVectors() {
        return vectors;
    }

    public Embedding() {
        this.vectors = new HashMap<>();
        this.embedding_size = -1;
    }

    // Ajoute un mot à l'embedding
    public void addWord(String word, Vector vector) {
        vectors.put(word, vector);
        if (embedding_size < 0) embedding_size = vector.getLength();
    }

    // Renvoie le vecteur associé à un mot (ou le vecteur nul si le mot n'est pas compris dans le dictionnaire)
    public Vector getWordEmbedding(String word) {
        if (containsWord(word)) return vectors.get(word);
        return Vector.nullVector(embedding_size);
    }

    // Renvoie la vectorisation d'une phrase (moyenne des vecteurs de ses mots)
    public Vector getSentenceEmbedding(String sentence) {
        sentence = Classifier.formatMessage(sentence); // On formate la phrase pour enlever les prépositions, les majuscules, la ponctuation
        int word_count = sentence.split(" ").length; // On compte combien on a de mots dedans
        Vector result = Vector.nullVector(embedding_size);
        for (String word : sentence.split(" ")) { // On parcourt tous les mots et on fait la somme de leur vectorisation
            result = result.add(getWordEmbedding(word));
        }
        result = result.divideByConstant(word_count);
        return result;
    }

    // Indique si le mot appartient au dictionnaire
    public boolean containsWord(String word) {
        return vectors.containsKey(word);
    }

}
