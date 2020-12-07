package fr.insa.kern.projet.classifying;


// Classe d'encapsulation d'un vecteur représentant un mot
public class Vector {

    // Vecteur à proprement parler et sa taille
    private final double[] values;
    private final int length;

    // ACCESSEURS
    public double[] getValues() {
        return values;
    }
    public int getLength() {
        return length;
    }
    public double getValue(int i) {
        return values[i];
    }
    public void setValue(int i, double v) {
        values[i] = v;
    }

    public Vector(double[] values) {
        this.values = values;
        this.length = this.values.length;
    }

    // Renvoie le module du vecteur
    public double getMagnitude() {
        double result = 0.0;
        for (int i = 0; i < length; i++) { // On parcourt le vecteur en faisant la somme des carrés des termes
            result += values[i] * values[i];
        }
        return Math.sqrt(result); // On prend la racine carrée du résultat
    }
    // Renvoie un nouveau vecteur qui est la somme du vecteur courant avec le vecteur V
    public Vector add(Vector V) {
        Vector out = new Vector(new double[length]);
        for (int i = 0; i < length; i++) {
            out.setValue(i, values[i] + V.getValue(i));
        }
        return out;
    }
    // Renvoie le produit scalaire du vecteur courant et du vecteur V sous la forme d'un nouveau vecteur
    public double dot(Vector V) {
        double result = 0.0;
        for (int i = 0; i < length; i++) { // On parcourt le vecteur en faisant la somme des carrés des termes
            result += values[i] * V.getValue(i);
        }
        return result; // On prend la racine carrée du résultat
    }
    // Calcule la similarité cosinus entre le vecteur courant et le vecteur V
    public double cosineSimilarity(Vector V) {
        double magnitudes = getMagnitude() * V.getMagnitude();
        if (magnitudes == 0.0) {
            return 0.0;
        } else {
            return dot(V) / (getMagnitude() * V.getMagnitude());
        }
    }
    // Renvoie un nouveau vecteur égal au vecteur courant avec toutes les valeurs divisées par une constante
    public Vector divideByConstant(double d) {
        Vector out = nullVector(length);
        for (int i = 0; i < length; i++) {
            out.setValue(i, values[i] / d);
        }
        return out;
    }

    // Méthode statique qui renvoie un vecteur de taille spécifiée rempli de 0
    public static Vector nullVector(int length) {
        return new Vector(new double[length]);
    }

}
