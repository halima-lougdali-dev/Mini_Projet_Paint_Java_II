import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/** Fenetre principale, menus et barre d outils. */
public class PaintFrame extends JFrame {
    private final DrawingPanel dessin = new DrawingPanel();
    private final JLabel statut = new JLabel("Outil : Crayon | Dessinez avec le bouton gauche de la souris.");
    private final JFileChooser fichiers = new JFileChooser();

    public PaintFrame() {
        super("Mini Paint - Java II");
        setLayout(new BorderLayout());
        setJMenuBar(creerMenus());
        add(creerBarreOutils(), BorderLayout.NORTH);
        add(new JScrollPane(dessin), BorderLayout.CENTER);
        statut.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(statut, BorderLayout.SOUTH);
        fichiers.setFileFilter(new FileNameExtensionFilter("Images PNG et JPG", "png", "jpg", "jpeg"));
        setSize(1050, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JMenuBar creerMenus() {
        JMenuBar barre = new JMenuBar();
        JMenu fichier = new JMenu("Fichier");
        JMenu edition = new JMenu("Edition");
        JMenu aide = new JMenu("Aide");
        fichier.setMnemonic('F');
        edition.setMnemonic('E');
        JMenuItem nouveau = new JMenuItem("Nouveau / Effacer");
        JMenuItem ouvrir = new JMenuItem("Ouvrir une image...");
        JMenuItem enregistrer = new JMenuItem("Enregistrer en PNG...");
        JMenuItem quitter = new JMenuItem("Quitter");
        JMenuItem annuler = new JMenuItem("Annuler");
        nouveau.addActionListener(e -> effacerAvecConfirmation());
        ouvrir.addActionListener(e -> ouvrirImage());
        enregistrer.addActionListener(e -> enregistrerImage());
        quitter.addActionListener(e -> dispose());
        annuler.addActionListener(e -> dessin.annuler());
        annuler.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        enregistrer.setAccelerator(KeyStroke.getKeyStroke("control S"));
        ouvrir.setAccelerator(KeyStroke.getKeyStroke("control O"));
        fichier.add(nouveau);
        fichier.add(ouvrir);
        fichier.add(enregistrer);
        fichier.addSeparator();
        fichier.add(quitter);
        edition.add(annuler);
        JMenuItem apropos = new JMenuItem("A propos");
        apropos.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Mini Paint - Projet Java II\nSwing, Graphics2D, souris et fichiers PNG.",
                "A propos", JOptionPane.INFORMATION_MESSAGE));
        aide.add(apropos);
        barre.add(fichier);
        barre.add(edition);
        barre.add(aide);
        return barre;
    }

    private JToolBar creerBarreOutils() {
        JToolBar barre = new JToolBar("Outils");
        barre.setFloatable(false);
        barre.setRollover(true);
        ButtonGroup outils = new ButtonGroup();
        ajouterOutil(barre, outils, "Crayon", DrawingPanel.Tool.CRAYON, true);
        ajouterOutil(barre, outils, "Gomme", DrawingPanel.Tool.GOMME, false);
        ajouterOutil(barre, outils, "Ligne", DrawingPanel.Tool.LIGNE, false);
        ajouterOutil(barre, outils, "Rectangle", DrawingPanel.Tool.RECTANGLE, false);
        ajouterOutil(barre, outils, "Ellipse", DrawingPanel.Tool.ELLIPSE, false);
        barre.addSeparator();
        JButton couleur = new JButton("Couleur...");
        couleur.addActionListener(e -> choisirCouleur());
        barre.add(couleur);
        barre.add(new JLabel("  Epaisseur : "));
        JSpinner taille = new JSpinner(new SpinnerNumberModel(4, 1, 60, 1));
        taille.setMaximumSize(new Dimension(66, 30));
        taille.addChangeListener(e -> dessin.setEpaisseur((Integer) taille.getValue()));
        barre.add(taille);
        JCheckBox remplir = new JCheckBox("Formes remplies");
        remplir.addActionListener(e -> dessin.setRempli(remplir.isSelected()));
        barre.add(remplir);
        barre.addSeparator();
        JButton annuler = new JButton("Annuler");
        annuler.addActionListener(e -> dessin.annuler());
        barre.add(annuler);
        JButton effacer = new JButton("Effacer");
        effacer.addActionListener(e -> effacerAvecConfirmation());
        barre.add(effacer);
        return barre;
    }

    private void ajouterOutil(JToolBar barre, ButtonGroup groupe, String nom,
                              DrawingPanel.Tool outil, boolean selectionne) {
        JToggleButton bouton = new JToggleButton(nom, selectionne);
        bouton.addActionListener(e -> {
            dessin.setOutil(outil);
            statut.setText("Outil : " + nom + " | Cliquez et glissez pour dessiner.");
        });
        groupe.add(bouton);
        barre.add(bouton);
    }

    private void choisirCouleur() {
        Color nouvelle = JColorChooser.showDialog(this, "Choisir une couleur", dessin.getCouleur());
        if (nouvelle != null) {
            dessin.setCouleur(nouvelle);
            statut.setText("Couleur choisie : " + String.format("#%06X", nouvelle.getRGB() & 0xFFFFFF));
        }
    }

    private void effacerAvecConfirmation() {
        int reponse = JOptionPane.showConfirmDialog(this,
                "Effacer tout le dessin ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (reponse == JOptionPane.YES_OPTION) dessin.effacer();
    }

    private void ouvrirImage() {
        if (fichiers.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            dessin.ouvrir(fichiers.getSelectedFile());
            statut.setText("Image ouverte : " + fichiers.getSelectedFile().getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur d'ouverture", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enregistrerImage() {
        if (fichiers.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File cible = fichiers.getSelectedFile();
        if (!cible.getName().toLowerCase().endsWith(".png")) {
            cible = new File(cible.getParentFile(), cible.getName() + ".png");
        }
        if (cible.exists()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Remplacer le fichier existant ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        try {
            dessin.enregistrer(cible);
            statut.setText("Image enregistree : " + cible.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Image enregistree en PNG !");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur d'enregistrement", JOptionPane.ERROR_MESSAGE);
        }
    }
}
