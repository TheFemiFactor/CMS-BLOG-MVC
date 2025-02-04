<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>Index</title>
        <link href="${pageContext.request.contextPath}/css/bootstrap.min.css" rel="stylesheet">
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/img/icon.png">

        <style>
            body {
                background-color: lightsteelblue;
            }
            .blog-post {
                background-color: white;
                border: 2px solid gray;
                padding: 20px;
                margin-bottom: 10px;
                border-radius: 5px;
            }
            h1, h2, h3, h4  {
                margin-left: 10px;
                font-family: "Times New Roman", "Times", serif;
                font-weight: bold;
            }
            #header {
                margin-bottom: 40px;
            }
            a {
                color: royalblue;
            }
            #post-content {
                white-space: normal;
            }
        </style> 

    </head>
    <body>

        <!--HEADER-->
        <div id="header" class="container">
            <%@include file="jsp/pageMenuFragment.jsp" %>
            <h1>${blogTitle}</h1>
            <hr/>
        </div>

        <div class="container">

            <!--POST LIST - LEFT COLUMN -->
            <c:choose>
                <c:when test="${fn:length(blogList) <= 0}">
                    <div class="col-sm-10">
                        No posts found
                    </div>
                </c:when>

                <c:otherwise>
                    <div class="col-sm-10">
                        <div class="blog-posts-block">
                            <c:if test="${tagView == 1}">
                                <h2>#${term}:</h2>
                            </c:if>
                            <br/>
                            <c:forEach var="post" items="${blogList}">
                                <div class="blog-post row">
                                    <div class="col-sm-12">
                                        <h3>${post.postTitle}</h3>
                                        <p id="post-info">by ${post.postUserName} on
                                            <fmt:formatDate pattern="MM/dd/yyyy" value="${post.postDate}"></fmt:formatDate>&nbsp;&nbsp;&nbsp;
                                                <br />
                                            <c:if test="${fn:length(post.postCategories) > 0}">
                                                Categories: 
                                                <c:forEach var="category" items="${post.postCategories}">  
                                                    <a href="${pageContext.request.contextPath}/category/${category}">${category}</a> 
                                                </c:forEach>
                                            </c:if>
                                            &nbsp;&nbsp;&nbsp;
                                            <c:if test="${fn:length(post.postTags) > 0}">
                                                Tags:
                                                <c:forEach var="tag" items="${post.postTags}"> 
                                                    <a href="${pageContext.request.contextPath}/tag/${tag}">${tag}</a>
                                                </c:forEach>
                                            </c:if>
                                            <br /><br />
                                            ${post.postContent}
                                        </p>
                                        <p>
                                            <a class="btn btn-primary" href="${pageContext.request.contextPath}/post/${post.postId}">Read More</a>
                                        </p>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                        <div class="text-center">
                            <nav>
                                <ul class="pagination">
                                    <c:choose>
                                        <c:when test="${currentPage > 0}">
                                            <c:set var="previousPage" value="${currentPage - 1}" />
                                            <li>
                                                <a href="${pageContext.request.contextPath}/page/${previousPage}">
                                                    <span>&laquo;</span>
                                                </a>
                                            </li>
                                        </c:when>
                                        <c:otherwise>
                                            <li class="disabled">
                                                <span>&laquo;</span>
                                            </li>
                                        </c:otherwise>
                                    </c:choose>

                                    <c:forEach begin="0" end="${numPages}" varStatus="page">
                                        <c:set var="pageNumber" value="${page.index + 1}" />

                                        <c:choose>
                                            <c:when test="${currentPage == page.index}">
                                                <li class="active"><a href="${pageContext.request.contextPath}/page/${page.index}">${pageNumber}</a></li>
                                                </c:when>
                                                <c:otherwise>
                                                <li><a href="${pageContext.request.contextPath}/page/${page.index}">${pageNumber}</a></li>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>

                                    <c:choose>
                                        <c:when test="${currentPage < numPages}">
                                            <c:set var="nextPage" value="${currentPage + 1}" />
                                            <li>
                                                <a href="${pageContext.request.contextPath}/page/${nextPage}">
                                                    <span>&raquo;</span>
                                                </a>
                                            </li>
                                        </c:when>
                                        <c:otherwise>
                                            <li class="disabled">
                                                <span>&raquo;</span>
                                            </li>
                                        </c:otherwise>
                                    </c:choose>
                                </ul>
                            </nav>
                        </div>
                    </div>

                </c:otherwise>
            </c:choose>
            <!--RIGHT COLUMN-->
            <div class="col-sm-2">
                <div class="row">
                    <div class="col-sm-12">
                        <h4><strong>Recent Posts</strong></h4>
                        <hr/>
                        <div id="recent-posts-body">
                            <ul>
                                <c:forEach var="recentpost" items="${recentPostList}">
                                    <li><a href="${pageContext.request.contextPath}/post/${recentpost.postId}">${recentpost.postTitle}</a></li>
                                    </c:forEach>
                            </ul>
                        </div>
                    </div>
                </div>
                <div class="row">
                    <div class="col-sm-12">
                        <hr/>
                        <h4><strong>Tags</strong></h4>
                        <hr/>
                        <div id="tags-body">
                            <c:forEach var="tag" items="${tags}">
                                <c:set var="fontSize" value="${((tag.term_count/5) * (2 - 0.65)) + 0.65}" />
                                <fmt:formatNumber var="fontSize" maxFractionDigits="2" value="${fontSize}" />
                                <a href="${pageContext.request.contextPath}/tag/${tag.term_name}" style="font-size: ${fontSize}em">${tag.term_name}</a>
                                &nbsp;
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>                
        </div>



        <!-- Placed at the end of the document so the pages load faster -->
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.1.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script> 
    </body>
</html>

