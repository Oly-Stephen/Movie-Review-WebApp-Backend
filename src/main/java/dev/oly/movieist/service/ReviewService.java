package dev.oly.movieist.service;

import dev.oly.movieist.model.Movie;
import dev.oly.movieist.model.Review;
import dev.oly.movieist.repository.ReviewRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Review createReview(String reviewBody, String imdbId) {
        Review review = repository.insert(new Review(reviewBody, LocalDateTime.now(), LocalDateTime.now()));

        mongoTemplate.update(Movie.class)
                .matching(Criteria.where("imdbId").is(imdbId))
                .apply(new Update().push("reviews").value(review))
                .first();
        return review;
    }

    public List<Review> findReviewsByMovie(String imdbId) {
        Movie movie = mongoTemplate.findOne(Query.query(Criteria.where("imdbId").is(imdbId)), Movie.class);
        if (movie != null) {
            List<ObjectId> reviewIds = movie.getReviews().stream().map(Review::getId).collect(Collectors.toList());
            return mongoTemplate.find(Query.query(Criteria.where("_id").in(reviewIds)), Review.class);
        }
        return Collections.emptyList();
    }
}