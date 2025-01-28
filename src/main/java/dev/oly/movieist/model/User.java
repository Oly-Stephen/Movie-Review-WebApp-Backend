package dev.oly.movieist.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@AllArgsConstructor @NoArgsConstructor
public class User {
    @Id
    private String id;

    private String email;
    private String name;
    private String provider; // "google", "github" etc...
    private String providerUserId; // Unique ID from the provider
    private String imageUrl; // Profile picture url
}