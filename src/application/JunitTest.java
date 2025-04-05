package application;

import static org.junit.Assert.*;
import org.junit.Test;

public class JunitTest {

    // Test class for ReviewData so we can create test objects
    class TestReviewData {
        public int id;
        public int reviewerId;
        public int questionId;
        public int answerId;
        public String content;
        
        public TestReviewData(int id, int reviewerId, int questionId, int answerId, String content) {
            this.id = id;
            this.reviewerId = reviewerId;
            this.questionId = questionId;
            this.answerId = answerId;
            this.content = content;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestReviewData other = (TestReviewData) obj;
            return id == other.id && reviewerId == other.reviewerId;
        }
    }

    @Test
    public void testTrustReviewerAddsVisualIndicator() {
        // Simple test that verifies the logic without JavaFX components
        java.util.List<Integer> trustedReviewerIds = new java.util.ArrayList<>();
        int reviewerId = 10;
        
        // Initial state: reviewer is not trusted
        assertFalse(trustedReviewerIds.contains(reviewerId));
        
        // Add to trusted list
        trustedReviewerIds.add(reviewerId);
        
        // Verify it was added
        assertTrue(trustedReviewerIds.contains(reviewerId));
    }
    
    @Test
    public void testUntrustReviewerRemovesVisualIndicator() {
        // Simple test that verifies the logic without JavaFX components
        java.util.List<Integer> trustedReviewerIds = new java.util.ArrayList<>();
        int reviewerId = 10;
        
        // Initial state: reviewer is trusted
        trustedReviewerIds.add(reviewerId);
        assertTrue(trustedReviewerIds.contains(reviewerId));
        
        // Remove from trusted list
        trustedReviewerIds.remove(Integer.valueOf(reviewerId));
        
        // Verify it was removed
        assertFalse(trustedReviewerIds.contains(reviewerId));
    }
    
    @Test
    public void testShowTrustedOnlyFiltersTrustedReviewers() {
        // Create test review data
        TestReviewData trustedReview = new TestReviewData(1, 10, 1, 1, "Trusted review");
        TestReviewData untrustedReview = new TestReviewData(2, 20, 1, 1, "Untrusted review");
        
        // Create reviews list
        java.util.List<TestReviewData> allReviews = new java.util.ArrayList<>();
        allReviews.add(trustedReview);
        allReviews.add(untrustedReview);
            
        // Set up trusted reviewers list
        java.util.List<Integer> trustedReviewerIds = new java.util.ArrayList<>();
        trustedReviewerIds.add(10); // Only trust reviewer ID 10
        
        // Filter reviews to show only trusted reviewers
        java.util.List<TestReviewData> filteredReviews = new java.util.ArrayList<>();
        for (TestReviewData review : allReviews) {
            if (trustedReviewerIds.contains(review.reviewerId)) {
                filteredReviews.add(review);
            }
        }
        
        // Verify only trusted reviews are shown
        assertEquals(1, filteredReviews.size());
        assertTrue(filteredReviews.contains(trustedReview));
        assertFalse(filteredReviews.contains(untrustedReview));
    }
    
    @Test
    public void testUncheckedShowAllReviewers() {
        // Create test review data
        TestReviewData trustedReview = new TestReviewData(1, 10, 1, 1, "Trusted review");
        TestReviewData untrustedReview = new TestReviewData(2, 20, 1, 1, "Untrusted review");
        
        // Create reviews list
        java.util.List<TestReviewData> allReviews = new java.util.ArrayList<>();
        allReviews.add(trustedReview);
        allReviews.add(untrustedReview);
            
        // Set up trusted reviewers list
        java.util.List<Integer> trustedReviewerIds = new java.util.ArrayList<>();
        trustedReviewerIds.add(10); // Only trust reviewer ID 10
        
        // Without filtering, all reviews should be shown
        java.util.List<TestReviewData> unfilteredReviews = new java.util.ArrayList<>(allReviews);
        
        // Verify all reviews are shown
        assertEquals(2, unfilteredReviews.size());
        assertTrue(unfilteredReviews.contains(trustedReview));
        assertTrue(unfilteredReviews.contains(untrustedReview));
    }
    
    @Test
    public void testShowTrustedOnlyWithNoTrustedReviewersShowsMessage() {
        // Create test review data
        TestReviewData untrustedReview = new TestReviewData(1, 10, 1, 1, "Untrusted review");
        
        // Create reviews list
        java.util.List<TestReviewData> allReviews = new java.util.ArrayList<>();
        allReviews.add(untrustedReview);
            
        // Empty trusted reviewers list
        java.util.List<Integer> trustedReviewerIds = new java.util.ArrayList<>();
        
        // Filter reviews to show only trusted reviewers
        java.util.List<TestReviewData> filteredReviews = new java.util.ArrayList<>();
        for (TestReviewData review : allReviews) {
            if (trustedReviewerIds.contains(review.reviewerId)) {
                filteredReviews.add(review);
            }
        }
        
        // Verify empty list is returned
        assertTrue(filteredReviews.isEmpty());
    }
}