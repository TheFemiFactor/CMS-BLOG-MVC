/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fo.finalcmsblogmvc.dao;

import com.fo.finalcmsblogmvc.dto.Comment;
import com.fo.finalcmsblogmvc.dto.Option;
import com.fo.finalcmsblogmvc.dto.Post;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


/**
 *
 * @author TheFemiFactor
 */
public class BlogDao implements BlogPostDaoInterface {

    private final String SQL_INSERT_POST = "INSERT INTO posts (post_user_id, post_type, post_title, post_content, post_status, post_date) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private final String SQL_UPDATE_POST = "UPDATE posts SET post_type = ?, post_title = ?, post_content=?, post_status=? WHERE post_id = ?";

    private final String SQL_DELETE_POST = "DELETE FROM posts WHERE post_id = ?";

    private final String SQL_SELECT_POST = "SELECT p.*, u.user_name FROM posts p JOIN users u ON p.post_user_id = u.user_id WHERE post_id = ?";

    private final String SQL_ADMIN_LIST_BLOG_POSTS = "SELECT p.*, u.user_name FROM posts p JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'blog'";

    private final String SQL_MARKETING_LIST_BLOG_POSTS = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'blog' "
            + "AND u.user_id IN (SELECT user_id FROM users WHERE user_role = 'ROLE_MARKETING')";

    private final String SQL_LIST_BLOG_POSTS = "SELECT * FROM posts WHERE post_type = 'blog' AND post_status = 'publish' ORDER BY post_date DESC";

    private final String SQL_COUNT_PUBLISHED_POSTS = "SELECT COUNT(post_id) FROM posts WHERE post_type = 'blog' AND post_status = 'publish'";

    private final String SQL_LIST_BLOG_POSTS_FOR_INDEX = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'blog' AND post_status = 'publish' "
            + "ORDER BY post_date DESC LIMIT 5 OFFSET ?";

    private final String SQL_SELECT_RECENT_POSTS = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'blog' AND post_status = 'publish' "
            + "ORDER BY post_date DESC LIMIT 5";

    private final String SQL_INSERT_COMMENT = "INSERT INTO comments (post_id, user_id, comment_author_name, comment_author_email, comment_content, comment_date, comment_author_website) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final String SQL_UPDATE_COMMENT = "UPDATE comments SET comment_status = ? WHERE comment_id = ?";

    private final String SQL_DELETE_COMMENT = "DELETE FROM comments WHERE comment_id = ?";

    private final String SQL_DELETE_COMMENTS_FOR_POST = "DELETE FROM comments WHERE post_id = ?";

    private final String SQL_SELECT_COMMENT = "SELECT * FROM comments WHERE comment_id = ?";

    private final String SQL_LIST_COMMENTS_BY_POST_ID = "SELECT * FROM comments WHERE post_id = ? ORDER by comment_date DESC";

    private final String SQL_ADMIN_LIST_STATIC_PAGES = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'page'";

    private final String SQL_LIST_STATIC_PAGES = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'page' AND post_status = 'publish'";

    private final String SQL_MARKETING_LIST_STATIC_PAGES = "SELECT p.*, u.user_name FROM posts p "
            + "JOIN users u ON p.post_user_id = u.user_id WHERE post_type = 'page' "
            + "AND u.user_id IN (SELECT user_id FROM users WHERE user_role = 'ROLE_MARKETING')";

    private final String SQL_LIST_ALL_COMMENTS = "SELECT * FROM comments";

    private final String SQL_SELECT_TAGS_AND_COUNTS = "SELECT t.term_name, COUNT(tp.term_id) as term_count "
            + "FROM terms t JOIN terms_posts tp ON t.term_id = tp.term_id "
            + "WHERE t.term_type = 'tag' GROUP BY tp.term_id, t.term_name";

    private final String SQL_SELECT_ALL_TERMS_BY_TYPE = "SELECT term_name FROM terms WHERE term_type = ?";

    private final String SQL_SELECT_POSTS_BY_TERM = "select p.*, u.* from posts as p join terms_posts as tp on p.post_id = tp.post_id join terms as t on tp.term_id = t.term_id join users u on p.post_user_id = u.user_id where term_name = ? "
            + " AND term_type = ? AND post_status = 'publish' ORDER BY post_date DESC";

    private final String SQL_INSERT_TERM = "INSERT INTO terms (term_name, term_type) VALUES (?, ?)";

    private final String SQL_SELECT_TERM_ID = "SELECT term_id FROM terms WHERE term_name = ? AND term_type = ?";

    private final String SQL_INSERT_TERMS_POSTS = "INSERT INTO terms_posts (term_id, post_id) VALUES (?, ?)";

    private final String SQL_SELECT_TERMS_BY_POST_ID = "SELECT term_name FROM terms WHERE term_type = ? "
            + "AND term_id IN (SELECT term_id FROM terms_posts WHERE post_id = ?)";

    private final String SQL_DELETE_TERMS_FROM_POST = "DELETE FROM terms_posts WHERE post_id = ?";

    private final String SQL_DELETE_UNUSED_TAGS = "DELETE FROM terms WHERE term_id NOT IN (SELECT DISTINCT term_id FROM terms_posts)";

    private final String SQL_SELECT_USER_ID_BY_USER_NAME = "SELECT user_id FROM users WHERE user_name = ?";
    
    private final String SQL_SELECT_OPTION_BY_OPTION_NAME = "SELECT * FROM options WHERE option_name = ?";
    
    private final String SQL_INSERT_OPTION = "INSERT INTO options (option_name, option_value) VALUES (?, ?)";
    
    private final String SQL_DELETE_OPTION = "DELETE FROM options WHERE option_name = ?";

    //2a: Declare JdbcTemplate Reference - the instance will be handed to us by Spring
    private JdbcTemplate jdbcTemplate;

    //2b: use Setter Injection to direct Spring to hand us an isntance of JdbcTemplate
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        // creating default users
//        jdbcTemplate.update("INSERT IGNORE INTO users (user_name, user_display_name, user_password, user_role) VALUES ('admin', 'Administrator', 'password', 'admin')");
//        jdbcTemplate.update("INSERT IGNORE INTO users (user_name, user_display_name, user_password, user_role) VALUES ('marketing', 'Marketing', 'password', 'marketing')");
    }

    @Override
    public void deleteCommentsForPost(int postId) {
        jdbcTemplate.update(SQL_DELETE_COMMENTS_FOR_POST, postId);
    }

    @Override
    public void deletePost(int postId) {
        deleteCommentsForPost(postId);
        jdbcTemplate.update(SQL_DELETE_POST, postId);
    }

    @Override
    public void updatePost(Post post, int postId) {
        jdbcTemplate.update(SQL_UPDATE_POST, post.getPostType(), post.getPostTitle(), post.getPostContent(), post.getPostStatus(), postId);
    }

    @Override
    public Post getPost(int postId) {
        try {
            Post post = jdbcTemplate.queryForObject(SQL_SELECT_POST, new PostMapper(), postId);
            post.setPostCategories(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "category")));
            post.setPostTags(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "tag")));

            return post;

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<String> getTermsByType(String type) {
        return jdbcTemplate.queryForList(SQL_SELECT_ALL_TERMS_BY_TYPE, String.class, type);
    }

    @Override
    public List<Post> listPosts() {
        return jdbcTemplate.query(SQL_LIST_BLOG_POSTS, new PostMapper());
    }

    @Override
    public List<Post> listPostsAdminPortal(String userRole) {
        if (userRole.equals("ROLE_ADMIN")) {
            return jdbcTemplate.query(SQL_ADMIN_LIST_BLOG_POSTS, new PostMapper());
        } else if (userRole.equals("ROLE_MARKETING")) {
            return jdbcTemplate.query(SQL_MARKETING_LIST_BLOG_POSTS, new PostMapper());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Post> listPostsByTerm(String termName, String termType) {
        List<Post> posts = jdbcTemplate.query(SQL_SELECT_POSTS_BY_TERM, new PostMapper(), termName, termType);
        for (Post post : posts) {
            Document content = Jsoup.parse(post.getPostContent().trim());
            String[] contentArray = content.text().split(" ");
            String exerpt = "";
            int limit = contentArray.length > 50 ? 50 : contentArray.length;

            for (int i = 0; i < limit; i++) {
                exerpt += contentArray[i] + " ";
            }
            if (contentArray.length > 50) {
                exerpt += "&hellip;";
            }
            post.setPostContent(exerpt);
            post.setPostCategories(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "category")));
            post.setPostTags(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "tag")));
        }
        return posts;
    }

    @Override
    public List<Post> listPostsForIndex(int offset) {
        List<Post> posts = jdbcTemplate.query(SQL_LIST_BLOG_POSTS_FOR_INDEX, new PostMapper(), offset);

        for (Post post : posts) {
            Document content = Jsoup.parse(post.getPostContent().trim());
            String[] contentArray = content.text().split(" ");
            String exerpt = "";
            int limit = contentArray.length > 50 ? 50 : contentArray.length;

            for (int i = 0; i < limit; i++) {
                exerpt += contentArray[i] + " ";
            }
            if (contentArray.length > 50) {
                exerpt += "&hellip;";
            }
            post.setPostContent(exerpt);
            post.setPostCategories(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "category")));
            post.setPostTags(StringUtils.collectionToCommaDelimitedString(getTermsForPost(post.getPostId(), "tag")));
        }
        return posts;
    }

    @Override
    public int countPublishedPosts() {
        return jdbcTemplate.queryForObject(SQL_COUNT_PUBLISHED_POSTS, Integer.class);
    }

    @Override
    public List<Post> listRecentPosts() {
        try {
            return jdbcTemplate.query(SQL_SELECT_RECENT_POSTS, new PostMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Post> listPages() {
        return jdbcTemplate.query(SQL_LIST_STATIC_PAGES, new PostMapper());
    }

    @Override
    public List<Post> listPagesAdminPortal(String userRole) {
        if (userRole.equals("ROLE_ADMIN")) {
            return jdbcTemplate.query(SQL_ADMIN_LIST_STATIC_PAGES, new PostMapper());
        } else if (userRole.equals("ROLE_MARKETING")) {
            return jdbcTemplate.query(SQL_MARKETING_LIST_STATIC_PAGES, new PostMapper());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public Comment addComment(Comment comment) {
        jdbcTemplate.update(SQL_INSERT_COMMENT,
                comment.getPostId(),
                comment.getUserId(),
                comment.getCommentAuthorName(),
                comment.getCommentEmail(),
                comment.getCommentContent(),
                comment.getCommentDate(),
                comment.getCommentWebsite()
        );
        comment.setCommentId(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class));
        return comment;
    }

    @Override
    public void deleteComment(int commentId) {
        jdbcTemplate.update(SQL_DELETE_COMMENT, commentId);
    }

    @Override
    public Comment getComment(int commentId) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_COMMENT, new CommentMapper(), commentId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Comment> listCommentsForPost(int postId) {
        return jdbcTemplate.query(SQL_LIST_COMMENTS_BY_POST_ID, new CommentMapper(), postId);
    }

    @Override
    public void updateComment(Comment comment) {
        jdbcTemplate.update(SQL_UPDATE_COMMENT, comment.getCommentStatus(), comment.getCommentId());
    }

    @Override
    public List<Map<String, Object>> getAllTagsAndCount() {
        try {
            return jdbcTemplate.queryForList(SQL_SELECT_TAGS_AND_COUNTS);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Comment> listAllComments() {
        return jdbcTemplate.query(SQL_LIST_ALL_COMMENTS, new CommentMapper());
    }

    @Override
    public List<String> getAllTerms(String termType) {
        try {
            return jdbcTemplate.queryForList(SQL_SELECT_ALL_TERMS_BY_TYPE, String.class, termType);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void addTerms(List<String> categories, List<String> tags) {

        for (int i = 0; i < categories.size(); i++) {
            try {
                jdbcTemplate.queryForObject(SQL_SELECT_TERM_ID, Integer.class, categories.get(i), "category");
            } catch (EmptyResultDataAccessException e) {
                if (!categories.get(i).isEmpty()) {
                    jdbcTemplate.update(SQL_INSERT_TERM, categories.get(i).trim(), "category");
                }
            }
        }

        for (int i = 0; i < tags.size(); i++) {
            try {
                jdbcTemplate.queryForObject(SQL_SELECT_TERM_ID, Integer.class, tags.get(i), "tag");
            } catch (EmptyResultDataAccessException e) {
                if (!tags.get(i).isEmpty()) {
                    jdbcTemplate.update(SQL_INSERT_TERM, tags.get(i).trim(), "tag");
                }
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void addPostToTerms(int postId, List<String> categories, List<String> tags) {
        List<Integer> categoryIds = new ArrayList<>();
        List<Integer> tagIds = new ArrayList<>();

        // first we add the terms id to our lists
        for (int i = 0; i < categories.size(); i++) {
            try {
                categoryIds.add(jdbcTemplate.queryForObject(SQL_SELECT_TERM_ID, Integer.class, categories.get(i).trim(), "category"));
            } catch (EmptyResultDataAccessException e) {
            }

        }
        for (int i = 0; i < tags.size(); i++) {
            try {
                tagIds.add(jdbcTemplate.queryForObject(SQL_SELECT_TERM_ID, Integer.class, tags.get(i).trim(), "tag"));
            } catch (EmptyResultDataAccessException e) {
            }

        }

        // next, we add the posts and term ids to the terms_posts table using batch update
        jdbcTemplate.batchUpdate(SQL_INSERT_TERMS_POSTS, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, categoryIds.get(i));
                ps.setInt(2, postId);
            }

            @Override
            public int getBatchSize() {
                return categoryIds.size();
            }

        });

        jdbcTemplate.batchUpdate(SQL_INSERT_TERMS_POSTS, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, tagIds.get(i));
                ps.setInt(2, postId);
            }

            @Override
            public int getBatchSize() {
                return tagIds.size();
            }

        });
    }

    @Override
    public List<String> getTermsForPost(int postId, String termType) {
        try {
            return jdbcTemplate.queryForList(SQL_SELECT_TERMS_BY_POST_ID, String.class, termType, postId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void deleteTermFromPost(int postId) {
        jdbcTemplate.update(SQL_DELETE_TERMS_FROM_POST, postId);

        // if terms are no longer linked to any posts, delete them from the terms table
        deleteUnusedTags();
    }

    @Override
    public void deleteUnusedTags() {
        jdbcTemplate.update(SQL_DELETE_UNUSED_TAGS);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void addPost(Post post) {
        jdbcTemplate.update(SQL_INSERT_POST,
                post.getPostUserId(),
                post.getPostType(),
                post.getPostTitle(),
                post.getPostContent(),
                post.getPostStatus(),
                post.getPostDate()
        );

        post.setPostId(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class));
    }

    @Override
    public int getUserId(String userName) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_USER_ID_BY_USER_NAME, Integer.class, userName);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @Override
    public Option getOption(String optionName) {
        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_OPTION_BY_OPTION_NAME, new OptionMapper(), optionName);
        } catch (EmptyResultDataAccessException e) {
            return new Option();
        }
    }

    @Override
    public void setOption(Option option) {
        try {
            jdbcTemplate.queryForObject(SQL_SELECT_OPTION_BY_OPTION_NAME, new OptionMapper(), "blogTitle");
            jdbcTemplate.update(SQL_DELETE_OPTION, "blogTitle");
        } catch (EmptyResultDataAccessException e) {
            
        } finally {
            jdbcTemplate.update(SQL_INSERT_OPTION, "blogTitle", option.getOptionValue());
        }
    }
    
    private class OptionMapper implements ParameterizedRowMapper<Option> {

        @Override
        public Option mapRow(ResultSet rs, int i) throws SQLException {
            Option option = new Option();
            option.setOptionName(rs.getString("option_name"));
            option.setOptionValue(rs.getString("option_value"));
            
            return option;
        }
        
    }

    private class CommentMapper implements ParameterizedRowMapper<Comment> {

        @Override
        public Comment mapRow(ResultSet rs, int i) throws SQLException {
            Timestamp commentDate = rs.getTimestamp("comment_date");
            Comment comment = new Comment();
            comment.setCommentId(rs.getInt("comment_id"));
            comment.setPostId(rs.getInt("post_id"));
            Post post = getPost(comment.getPostId());
            comment.setPostTitle(post.getPostTitle());
            comment.setUserId(rs.getInt("user_id"));
            comment.setCommentAuthorName(rs.getString("comment_author_name"));
            comment.setCommentEmail(rs.getString("comment_author_email"));
            comment.setCommentWebsite(rs.getString("comment_author_website"));
            comment.setCommentContent(rs.getString("comment_content"));
            comment.setCommentStatus(rs.getString("comment_status"));
            comment.setCommentDate(commentDate);
            return comment;
        }
    }

    private class PostMapper implements ParameterizedRowMapper<Post> {

        @Override
        public Post mapRow(ResultSet rs, int i) throws SQLException {
            Timestamp postDate = rs.getTimestamp("post_date");
            Post post = new Post();
            post.setPostId(rs.getInt("post_id"));
            post.setPostUserId(rs.getInt("post_user_id"));
            post.setPostUserName(rs.getString("user_name"));
            post.setPostType(rs.getString("post_type"));
            post.setPostTitle(rs.getString("post_title"));
            post.setPostContent(rs.getString("post_content"));
            post.setPostStatus(rs.getString("post_status"));
            post.setPostDate(postDate);
            return post;
        }
    }
}

