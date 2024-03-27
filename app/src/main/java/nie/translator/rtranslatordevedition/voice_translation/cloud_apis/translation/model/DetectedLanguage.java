package nie.translator.rtranslatordevedition.voice_translation.cloud_apis.translation.model;

public class DetectedLanguage {
    String language;
    Double score;

    @Override
    public String toString() {
        return "DetectedLanguage{" +
                "language='" + language + '\'' +
                ", score=" + score +
                '}';
    }

    public DetectedLanguage(String language, Double score) {
        this.language = language;
        this.score = score;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
