import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/** Zone de dessin : une image en memoire et des evenements souris. */
public class DrawingPanel extends JPanel {
    public enum Tool { CRAYON, GOMME, LIGNE, RECTANGLE, ELLIPSE }

    private BufferedImage image;
    private Color couleur = Color.BLACK;
    private int epaisseur = 4;
    private Tool outil = Tool.CRAYON;
    private Point debut;
    private Point precedent;
    private Point courant;
    private boolean dessinEnCours;
    private boolean rempli;
    private final Deque<BufferedImage> historique = new ArrayDeque<>();
    private static final int MAX_HISTORIQUE = 15;

    public DrawingPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1100, 680));
        setOpaque(true);
        MouseAdapter souris = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                verifierImage();
                memoriserEtat();
                debut = e.getPoint();
                precedent = debut;
                courant = debut;
                dessinEnCours = true;
                if (outil == Tool.CRAYON || outil == Tool.GOMME) dessinerTrait(debut, debut);
                repaint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (!dessinEnCours) return;
                courant = e.getPoint();
                if (outil == Tool.CRAYON || outil == Tool.GOMME) {
                    dessinerTrait(precedent, courant);
                    precedent = courant;
                }
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!dessinEnCours) return;
                courant = e.getPoint();
                if (outil == Tool.LIGNE || outil == Tool.RECTANGLE || outil == Tool.ELLIPSE) {
                    Graphics2D g2 = image.createGraphics();
                    parametrer(g2);
                    g2.setColor(couleur);
                    g2.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    dessinerForme(g2, debut, courant);
                    g2.dispose();
                } else {
                    dessinerTrait(precedent, courant);
                }
                dessinEnCours = false;
                debut = null;
                courant = null;
                repaint();
            }
        };
        addMouseListener(souris);
        addMouseMotionListener(souris);
    }

    private void verifierImage() {
        int largeur = Math.max(1, getWidth());
        int hauteur = Math.max(1, getHeight());
        if (image == null || image.getWidth() < largeur || image.getHeight() < hauteur) {
            int w = Math.max(largeur, image == null ? 1 : image.getWidth());
            int h = Math.max(hauteur, image == null ? 1 : image.getHeight());
            BufferedImage nouvelle = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = nouvelle.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            if (image != null) g2.drawImage(image, 0, 0, null);
            g2.dispose();
            image = nouvelle;
        }
    }

    private void parametrer(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void dessinerTrait(Point a, Point b) {
        Graphics2D g2 = image.createGraphics();
        parametrer(g2);
        g2.setColor(outil == Tool.GOMME ? Color.WHITE : couleur);
        g2.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(a.x, a.y, b.x, b.y);
        g2.dispose();
    }

    private void dessinerForme(Graphics2D g2, Point a, Point b) {
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        int w = Math.abs(a.x - b.x);
        int h = Math.abs(a.y - b.y);
        switch (outil) {
            case LIGNE -> g2.drawLine(a.x, a.y, b.x, b.y);
            case RECTANGLE -> { if (rempli) g2.fillRect(x, y, w, h); else g2.drawRect(x, y, w, h); }
            case ELLIPSE -> { if (rempli) g2.fillOval(x, y, w, h); else g2.drawOval(x, y, w, h); }
            default -> { }
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        verifierImage();
        g.drawImage(image, 0, 0, null);
        if (dessinEnCours && debut != null && courant != null
                && outil != Tool.CRAYON && outil != Tool.GOMME) {
            Graphics2D g2 = (Graphics2D) g.create();
            parametrer(g2);
            g2.setColor(couleur);
            g2.setStroke(new BasicStroke(epaisseur, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            dessinerForme(g2, debut, courant);
            g2.dispose();
        }
    }

    private BufferedImage copieImage() {
        verifierImage();
        BufferedImage copie = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = copie.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return copie;
    }

    private void memoriserEtat() {
        if (historique.size() == MAX_HISTORIQUE) historique.removeFirst();
        historique.addLast(copieImage());
    }

    public void annuler() {
        if (!historique.isEmpty()) {
            image = historique.removeLast();
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Aucune action a annuler.");
        }
    }

    public void effacer() {
        memoriserEtat();
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.dispose();
        repaint();
    }

    public void ouvrir(File fichier) throws IOException {
        BufferedImage chargee = ImageIO.read(fichier);
        if (chargee == null) throw new IOException("Format d'image non pris en charge.");
        memoriserEtat();
        int w = Math.max(chargee.getWidth(), Math.max(1, getWidth()));
        int h = Math.max(chargee.getHeight(), Math.max(1, getHeight()));
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        g2.drawImage(chargee, 0, 0, null);
        g2.dispose();
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }

    public void enregistrer(File fichier) throws IOException {
        verifierImage();
        if (!ImageIO.write(image, "png", fichier)) throw new IOException("Impossible d'ecrire le PNG.");
    }

    public Color getCouleur() { return couleur; }
    public void setCouleur(Color couleur) { if (couleur != null) this.couleur = couleur; }
    public void setEpaisseur(int epaisseur) { this.epaisseur = Math.max(1, epaisseur); }
    public void setOutil(Tool outil) { this.outil = outil; }
    public Tool getOutil() { return outil; }
    public void setRempli(boolean rempli) { this.rempli = rempli; }
}
