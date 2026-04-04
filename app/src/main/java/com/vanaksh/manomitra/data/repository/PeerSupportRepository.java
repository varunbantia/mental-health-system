package com.vanaksh.manomitra.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.data.model.Reply;
import com.vanaksh.manomitra.data.model.Report;

public class PeerSupportRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference postsRef = db.collection("posts");
    private final CollectionReference repliesRef = db.collection("replies");
    private final CollectionReference reportsRef = db.collection("reports");

    // Posts
    public CollectionReference getPostsRef() {
        return postsRef;
    }

    public Query getApprovedPosts() {
        return postsRef.whereEqualTo("status", "approved");
    }

    public Query getPostsByCategory(String category) {
        return postsRef.whereEqualTo("status", "approved")
                .whereEqualTo("category", category);
    }

    public void createPost(Post post) {
        String id = postsRef.document().getId();
        post.setPostId(id);
        postsRef.document(id).set(post);
    }

    // Replies
    public Query getRepliesForPost(String postId) {
        return repliesRef.whereEqualTo("postId", postId);
    }

    public void addReply(Reply reply) {
        String id = repliesRef.document().getId();
        reply.setReplyId(id);
        repliesRef.document(id).set(reply);
        
        // Increment reply count in post (Atomic update ideally, but simple for now)
        db.collection("posts").document(reply.getPostId()).update("replyCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    // Moderation
    public Query getPendingPosts() {
        return postsRef.whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public Query getFlaggedPosts() {
        return postsRef.whereEqualTo("status", "flagged")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public void updatePostStatus(String postId, String status) {
        postsRef.document(postId).update("status", status);
    }

    public void reportPost(Report report) {
        String id = reportsRef.document().getId();
        report.setReportId(id);
        reportsRef.document(id).set(report);
        
        // Mark post as flagged
        updatePostStatus(report.getPostId(), "flagged");
    }

    public void supportPost(String postId) {
        postsRef.document(postId).update("supportCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void supportReply(String replyId) {
        repliesRef.document(replyId).update("supportCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    public void unsupportPost(String postId) {
        postsRef.document(postId).update("supportCount", com.google.firebase.firestore.FieldValue.increment(-1));
    }

    public void unsupportReply(String replyId) {
        repliesRef.document(replyId).update("supportCount", com.google.firebase.firestore.FieldValue.increment(-1));
    }

    public void incrementReplyCount(String postId) {
        postsRef.document(postId).update("replyCount", com.google.firebase.firestore.FieldValue.increment(1));
    }
}
