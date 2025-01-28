package dev.oly.movieist.service;

import dev.oly.movieist.model.User;
import dev.oly.movieist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user details
        String provider = userRequest.getClientRegistration().getRegistrationId(); // e.g., "google", "github"
        String providerUserId = oauth2User.getAttribute("sub"); // Google sub or GitHub ID
        String name = oauth2User.getAttribute("name");
        String email = oauth2User.getAttribute("email");

        // For GitHub, fetch email if not available in the default response
        if ("github".equals(provider) && email == null) {
            email = fetchGitHubEmail(userRequest.getAccessToken().getTokenValue());
        }

        // Extract profile image URL
        String imageUrl = null;
        if ("google".equals(provider)) {
            imageUrl = oauth2User.getAttribute("picture"); // Google profile picture
        } else if ("github".equals(provider)) {
            imageUrl = oauth2User.getAttribute("avatar_url"); // GitHub profile picture
        }

        // Save or update user in the database
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> new User());
        user.setEmail(email);
        user.setName(name);
        user.setProvider(provider);
        user.setProviderUserId(providerUserId);
        user.setImageUrl(imageUrl); 
        userRepository.save(user);

        return oauth2User;
    }

    // Helper method to fetch GitHub email if not available in the default response
    private String fetchGitHubEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            for (Map<String, Object> emailEntry : response.getBody()) {
                if (Boolean.TRUE.equals(emailEntry.get("primary"))) {
                    return (String) emailEntry.get("email");
                }
            }
        }
        return null;
    }
}