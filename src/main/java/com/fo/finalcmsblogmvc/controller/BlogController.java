/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fo.finalcmsblogmvc.controller;

import com.octo.captcha.service.image.ImageCaptchaService;
import com.fo.finalcmsblogmvc.dao.BlogPostDaoInterface;
import com.fo.finalcmsblogmvc.dto.Comment;
import com.fo.finalcmsblogmvc.dto.Option;
import com.fo.finalcmsblogmvc.dto.Post;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author apprentice
 */
@Controller
public class BlogController {

    BlogPostDaoInterface dao;
    Authentication auth;
    String loggedInUser;
    String userRole;
    int countPublishedPosts;

    @Inject
    public BlogController(BlogPostDaoInterface dao) {
        this.dao = dao;
        countPublishedPosts = dao.countPublishedPosts();
    }

    @RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String homePage(Model model) {
        model.addAttribute("recentPostList", dao.listRecentPosts());
        model.addAttribute("tags", dao.getAllTagsAndCount());
        model.addAttribute("categories", dao.getTermsByType("category"));
        model.addAttribute("blogList", dao.listPostsForIndex(0));
        model.addAttribute("currentPage", 0);
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());

        int numPages = (int) Math.ceil(countPublishedPosts / 5);
        model.addAttribute("numPages", numPages);
        return "index";
    }

    @RequestMapping(value = "login", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayLogin() {
        return "login";
    }

    @RequestMapping(value = {"adminPortal", "adminBlogView"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayAdminPostView(Model model) {
        auth = SecurityContextHolder.getContext().getAuthentication();

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) auth.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                userRole = "ROLE_ADMIN";
                break;
            } else if (authority.getAuthority().equals("ROLE_MARKETING")) {
                userRole = "ROLE_MARKETING";
                break;
            } else {
                break;
            }
        }

        loggedInUser = auth.getName();
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());

        return "adminBlogView";
    }

    @RequestMapping(value = "adminPageView", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayAdminPageView(Model model) {
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "adminPageView";
    }

    ///PAGES AND POSTS ENDPOINTS///
    @RequestMapping(value = "addNewPost", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayAddNewPost(Model model) {
        model.addAttribute("postType", "blog");
        model.addAttribute("categoryList", dao.getAllTerms("category"));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "addPost";
    }

    @RequestMapping(value = "addNewPage", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayAddNewPage(Model model) {
        model.addAttribute("postType", "page");
        model.addAttribute("categoryList", dao.getAllTerms("category"));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "addPost";
    }

    @RequestMapping(value = "category/{category}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String getPostsForCategory(@PathVariable("category") String category, Model model) {
        model.addAttribute("blogList", dao.listPostsByTerm(category, "category"));
        model.addAttribute("recentPostList", dao.listRecentPosts());
        model.addAttribute("tags", dao.getAllTagsAndCount());
        model.addAttribute("categories", dao.getTermsByType("category"));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "index";
    }

    @RequestMapping(value = "displayEditView/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayEditView(@PathVariable int id, Model model) {
        Post post = dao.getPost(id);
        model.addAttribute("post", post);
        model.addAttribute("categoryList", Arrays.asList(post.getPostCategories().split(",")));
        model.addAttribute("tagList", Arrays.asList(post.getPostTags().split(",")));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());

        return "editPost";
    }

    @RequestMapping(value = "/page/{page}", method = RequestMethod.GET)
    public String homePageOffset(Model model, @PathVariable("page") int page) {
        model.addAttribute("recentPostList", dao.listRecentPosts());
        model.addAttribute("tags", dao.getAllTagsAndCount());
        model.addAttribute("currentPage", page);
        model.addAttribute("categories", dao.getTermsByType("category"));

        int numPages = (int) Math.ceil(countPublishedPosts / 5);
        model.addAttribute("numPages", numPages);

        int offset = page * 5;

        model.addAttribute("blogList", dao.listPostsForIndex(offset));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "index";
    }

    @RequestMapping(value = "page/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deletePage(@PathVariable int id) {
        dao.deletePost(id);
    }

    @RequestMapping(value = "posts", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Post> getBlogPosts() {
        return dao.listPosts();
    }

    @RequestMapping(value = "adminPosts", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Post> getBlogPostsAdminPortal() {
        return dao.listPostsAdminPortal(userRole);
    }

    @RequestMapping(value = "pages", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Post> getStaticPages() {
        return dao.listPages();
    }

    @RequestMapping(value = "adminPages", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Post> getStaticPagesAdminPortal() {
        return dao.listPagesAdminPortal(userRole);
    }

    @RequestMapping(value = "tag/{term}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String getPostsByTerm(@PathVariable("term") String term, Model model) {
        model.addAttribute("tagView", 1);
        model.addAttribute("term", term);
        model.addAttribute("recentPostList", dao.listRecentPosts());
        model.addAttribute("tags", dao.getAllTagsAndCount());
        model.addAttribute("categories", dao.getTermsByType("category"));
        model.addAttribute("blogList", dao.listPostsByTerm(term, "tag"));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "index";
    }

    @RequestMapping(value = "post/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deletePost(@PathVariable int id) {
        dao.deleteTermFromPost(id);
        dao.deletePost(id);
    }

    @RequestMapping(value = "post/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void updatePost(@RequestBody Post post) {
        dao.updatePost(post, post.getPostId());
        dao.deleteTermFromPost(post.getPostId());
        List<String> categories = new ArrayList<>();
        List<String> tags = Arrays.asList(post.getPostTags().split(","));

        if (post.getPostCategories().length() == 0) {
            categories.add("uncategorized");
        } else {
            categories = Arrays.asList(post.getPostCategories().split(","));
        }

        dao.addTerms(categories, tags);
        dao.addPostToTerms(post.getPostId(), categories, tags);

    }

    @RequestMapping(value = "post/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayPostPage(@PathVariable int id, Model model) {
        model.addAttribute("recentPostList", dao.listRecentPosts());
        model.addAttribute("categories", dao.getTermsByType("category"));
        model.addAttribute("tags", dao.getAllTagsAndCount());
        model.addAttribute("post", dao.getPost(id));
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "staticPage";
    }

    @RequestMapping(value = "post", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void addBlogPost(@RequestBody Post post) {
        if (post != null) {
            Date date = new Date();
            // we need to get the user that is currently posting
            post.setPostUserId(dao.getUserId(loggedInUser));
            post.setPostDate(date);

            dao.addPost(post);
            List<String> categories = new ArrayList<>();
            List<String> tags = Arrays.asList(post.getPostTags().split(","));

            if (post.getPostCategories().length() == 0) {
                categories.add("uncategorized");
            } else {
                categories = Arrays.asList(post.getPostCategories().split(","));
            }

            dao.addTerms(categories, tags);
            dao.addPostToTerms(post.getPostId(), categories, tags);
            dao.deleteUnusedTags();
        }
    }

    ///COMMENT ENDPOINTS///
    @RequestMapping(value = "adminCommentView", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String displayAdminCommentPage(Model model) {
        
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "adminCommentView";
    }

    @RequestMapping(value = "addComment", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public String addComment(HttpServletRequest req, @ModelAttribute("comment") Comment comment, RedirectAttributes redir) {
        boolean validCaptcha = instance.validateResponseForID(req.getSession().getId(), req.getParameter("captcha"));

        if (validCaptcha) {
            Date date = new Date();
            comment.setCommentDate(date);
            dao.addComment(comment);
            redir.addFlashAttribute("message", "Comment added. Waiting for approval");
        } else {
            redir.addFlashAttribute("message", "Invalid Captcha");
        }

        return "redirect:post/" + comment.getPostId();
    }

    @RequestMapping(value = "comments", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Comment> getAllComments() {
        return dao.listAllComments();
    }

    @RequestMapping(value = "comments/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Comment> getAllCommentsForPost(@PathVariable int id) {
        return dao.listCommentsForPost(id);
    }

    @RequestMapping(value = "approveComment/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void approveComment(@PathVariable int id) {
        Comment comment = dao.getComment(id);
        comment.setCommentStatus("approved");
        dao.updateComment(comment);
    }

    @RequestMapping(value = "unapproveComment/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void unapproveComment(@PathVariable int id) {
        Comment comment = dao.getComment(id);
        comment.setCommentStatus("unapproved");
        dao.updateComment(comment);
    }

    @RequestMapping(value = "adminOptions", method = RequestMethod.GET)
    public String adminOptions(Model model) {
        Option blogTitle = dao.getOption("blogTitle");
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        
        return "adminOptions";
    }
    
    @RequestMapping(value = "adminOptions", method = RequestMethod.POST)
    public String setAdminOptions(Model model, @ModelAttribute("option") Option option) {
        dao.setOption(option);
        Option blogTitle = dao.getOption("blogTitle");
        
        model.addAttribute("blogTitle", blogTitle.getOptionValue());
        model.addAttribute("message", "Option updated");
        
        
        return "adminOptions";
    }

    private static ImageCaptchaService instance;

    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void showCaptcha(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("jcaptcha.xml");
        instance = ctx.getBean("captchaService", ImageCaptchaService.class);

        byte[] captchaChallengeAsJpeg = null;

        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();

        String captchaId = req.getSession().getId();
        BufferedImage challenge = instance.getImageChallengeForID(captchaId, req.getLocale());

        ImageIO.write(challenge, "png", jpegOutputStream);

        captchaChallengeAsJpeg = jpegOutputStream.toByteArray();

        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);
        res.setContentType("image/png");

        ServletOutputStream responseOutputStream = res.getOutputStream();
        responseOutputStream.write(captchaChallengeAsJpeg);
        responseOutputStream.flush();
        responseOutputStream.close();

    }
}
