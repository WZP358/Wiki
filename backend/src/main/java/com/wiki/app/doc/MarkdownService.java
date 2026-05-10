package com.wiki.app.doc;

import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownService {
    private static final MutableDataSet OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, List.of(TaskListExtension.create()))
            .set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    private final Parser parser = Parser.builder(OPTIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();

    public String toHtml(String markdown) {
        return renderer.render(parser.parse(markdown == null ? "" : markdown));
    }
}
