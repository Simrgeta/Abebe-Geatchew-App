package com.abebegetachew.abebe.salarymanagement.abebegetachew;

public class FeedbackModel {
    public String email;
    public String feedback;
    public float rating;

    // Empty constructor for Firebase
    public FeedbackModel() {
    }

    public FeedbackModel(String email, String feedback, float rating) {
        this.email = email;
        this.feedback = feedback;
        this.rating = rating;
    }
}
