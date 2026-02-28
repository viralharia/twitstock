package com.example.twitstock.model;

import java.time.ZonedDateTime;

/**
 * Represents a single tweet returned by the Twitter data provider.
 *
 * <p>Fields {@code isReply} and {@code isRetweet} are populated from the
 * twitterapi.io response metadata and are used as a safety filter on top of
 * the server-side {@code -is:reply -is:retweet} query operators.</p>
 */
public class TweetDto {

    private String id;
    private String text;
    private ZonedDateTime createdAt;
    private String author;

    // Engagement
    private int likes;
    private int retweets;
    private int replies;

    // Safety-filter flags (from twitterapi.io response metadata)
    private boolean isReply;
    private boolean isRetweet;

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId()                    { return id; }
    public void setId(String id)             { this.id = id; }

    public String getText()                  { return text; }
    public void setText(String text)         { this.text = text; }

    public ZonedDateTime getCreatedAt()      { return createdAt; }
    public void setCreatedAt(ZonedDateTime c){ this.createdAt = c; }

    public String getAuthor()                { return author; }
    public void setAuthor(String author)     { this.author = author; }

    public int getLikes()                    { return likes; }
    public void setLikes(int likes)          { this.likes = likes; }

    public int getRetweets()                 { return retweets; }
    public void setRetweets(int retweets)    { this.retweets = retweets; }

    public int getReplies()                  { return replies; }
    public void setReplies(int replies)      { this.replies = replies; }

    public boolean isIsReply()               { return isReply; }
    public void setIsReply(boolean isReply)  { this.isReply = isReply; }

    public boolean isIsRetweet()             { return isRetweet; }
    public void setIsRetweet(boolean v)      { this.isRetweet = v; }
}