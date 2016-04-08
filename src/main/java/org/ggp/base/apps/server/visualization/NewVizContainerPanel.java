package org.ggp.base.apps.server.visualization;

import java.awt.Dimension;
import java.io.File;
import java.util.Set;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.JPanel;

import org.ggp.base.util.files.FileUtils;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.ui.GameStateRenderer;

public class NewVizContainerPanel extends JPanel {
    public NewVizContainerPanel(final Set<GdlSentence> stateContents, final String js, VisualizationPanel parent)
    {
        Dimension d = GameStateRenderer.getDefaultSize();
        setPreferredSize(d);

        final JFXPanel jfxPanel = new JFXPanel();
        this.add(jfxPanel);
        jfxPanel.setPreferredSize(d);
        //		this.pac;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                renderContents(jfxPanel, stateContents, js);
            }
        });
    }

    private static void renderContents(JFXPanel panel, Set<GdlSentence> stateContents, String js) {
        System.out.println("XML contents: " + stateContents);
        System.out.println("JS contents: " + js);

        Group group = new Group();
        Scene scene = new Scene(group);
        panel.setScene(scene);
        WebView webView = new WebView();
        group.getChildren().add(webView);
        webView.setMinSize(600, 600);

        //		webView.getEngine().load("http://www.google.com");
        //        webView.getEngine().load("file:///C:/Users/Alex/bitbucket/Alloy/Alloy/games/resources/canvasTest.html");
        File canvasFile = new File("games/resources/canvasTest.html");
        String canvasHtml = FileUtils.readFileAsString(canvasFile);
        canvasHtml = insertState(canvasHtml, stateContents);
        canvasHtml = insertScripts(canvasHtml, js);
        webView.getEngine().loadContent(canvasHtml);
        System.out.println("Loaded content");
    }

    private static String insertState(String canvasHtml,
            Set<GdlSentence> stateContents) {
        StringBuilder stateCode = new StringBuilder();
        for (GdlSentence sentence : stateContents) {

        }

        return canvasHtml.replace("<!-- STATE -->", stateCode.toString());
    }

    //TODO: Pass in JS filenames instead, reference those
    private static String insertScripts(String canvasHtml, String js) {
        return canvasHtml.replace("<!-- SCRIPTS -->", "<script>\n" + js + "\n</script>\n");
    }

}
