package xyz.btcland.libretube.obj;

import java.util.List;

public class CommentsPage {

    public List<Comment> comments;
    public String nextpage;
    public boolean disabled;

    public CommentsPage(List<Comment> comments, String nextpage, boolean disabled) {
        this.comments = comments;
        this.nextpage = nextpage;
        this.disabled = disabled;
    }
}
