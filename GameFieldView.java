package game.view; // Добавлен первый коммит main

import game.core.Game;
import game.core.GameField;
import game.model.Cell; // Второй коммит в ветку main
import game.model.Tile;
import game.model.FreezeMine;

import javax.swing.*;
import java.awt.*; // Третий коммит в ветку main
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class GameFieldView extends JPanel implements CellUpdateListener {

    private final GameField _field;
    private final Game _game;
    private Cell _emptyCell;
    private Map<Cell, CellWidget> _cellWidgets = new HashMap<>();
    private Timer _refreshTimer;

    public GameFieldView(GameField field, Game game) {
        _field = field;
        _game = game;

        setLayout(new GridLayout(_field.getHeight(), _field.getWidth()));

        Dimension fieldDimension = new Dimension(
                CellWidget.CELL_SIZE * _field.getWidth(),
                CellWidget.CELL_SIZE * _field.getHeight()
        );

        setPreferredSize(fieldDimension);
        setMinimumSize(fieldDimension);
        setMaximumSize(fieldDimension);

        findEmptyCell();

        // Создаем виджеты для всех клеток
        for (int y = 0; y < _field.getHeight(); y++) {
            for (int x = 0; x < _field.getWidth(); x++) {
                Cell cell = _field.getCell(x, y);
                CellWidget widget = new CellWidget(cell, this);
                widget.addMouseListener(new CellClickListener(cell));
                add(widget);
                _cellWidgets.put(cell, widget);
            }
        }

        setFocusable(true);

        // Таймер для принудительного обновления всех клеток каждые 500 мс
        _refreshTimer = new Timer(500, e -> refreshAllCells());
        _refreshTimer.start();
    }

    @Override
    public void onCellUpdated(Cell cell) {
        CellWidget widget = _cellWidgets.get(cell);
        if (widget != null) {
            widget.repaint();
        }
    }

    public void refreshAllCells() {
        for (Component comp : getComponents()) {
            if (comp instanceof CellWidget) {
                comp.repaint();
            }
        }
    }

    private void findEmptyCell() {
        for (int y = 0; y < _field.getHeight(); y++) {
            for (int x = 0; x < _field.getWidth(); x++) {
                Cell cell = _field.getCell(x, y);
                if (cell.getTile() == null) {
                    _emptyCell = cell;
                    //System.out.println("Найдена пустая клетка: (" + x + "," + y + ")");
                    return;
                }
            }
        }
    }

    private class CellClickListener extends MouseAdapter {
        private final Cell _cell;

        public CellClickListener(Cell cell) {
            _cell = cell;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Если клетка заморожена - нельзя кликать
            if (_cell.isFrozen()) {
                JOptionPane.showMessageDialog(
                        GameFieldView.this,
                        "Эта клетка заморожена! Подождите 10 секунд.",
                        "Клетка заморожена",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            Tile clickedTile = _cell.getTile();

            if (clickedTile != null) {
                if (_emptyCell == null) {
                    findEmptyCell();
                }

                if (_emptyCell != null && _cell.isNeighbor(_emptyCell)) {
                    if (_emptyCell.isFrozen()) {
                        JOptionPane.showMessageDialog(
                                GameFieldView.this,
                                "Пустая клетка заморожена! Нельзя двигать фишку туда.",
                                "Клетка заморожена",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    Cell fromCell = _cell;
                    Cell toCell = _emptyCell;

                    fromCell.extractUnit(clickedTile);
                    toCell.putUnit(clickedTile);
                    clickedTile.setOwner(toCell);

                    _emptyCell = fromCell;
                    _game.incrementMoves();

                    onCellUpdated(fromCell);
                    onCellUpdated(toCell);

                    if (_game.isOver()) {
                        _game.stop();
                        showGameResults();
                    }
                }
            }
        }
    }

    private void showGameResults() {
        _refreshTimer.stop();

        int totalSeconds = _game.getElapsedSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Победа!", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(54, 190, 82));
        JLabel congratsLabel = new JLabel("🎉 ПОЗДРАВЛЯЕМ! ВЫ ВЫИГРАЛИ! 🎉");
        congratsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        congratsLabel.setForeground(Color.WHITE);
        congratsLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        topPanel.add(congratsLabel);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        centerPanel.setBackground(new Color(252, 252, 196));

        JLabel timeLabel = new JLabel("⏱️ Время игры: " + String.format("%d мин %02d сек", minutes, seconds));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel movesLabel = new JLabel("🔄 Сделано ходов: " + _game.getMoveCount());
        movesLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        movesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        movesLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel ratingLabel = new JLabel();
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 18));
        ratingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ratingLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        int moves = _game.getMoveCount();
        if (moves < 50) {
            ratingLabel.setText("ОТЛИЧНО! Вы настоящий мастер!");
            ratingLabel.setForeground(new Color(255, 140, 0));
        } else if (moves < 100) {
            ratingLabel.setText("ХОРОШО! Можно и лучше!");
            ratingLabel.setForeground(new Color(0, 150, 0));
        } else {
            ratingLabel.setText("НЕПЛОХО! Попробуйте еще раз!");
            ratingLabel.setForeground(new Color(100, 100, 200));
        }

        centerPanel.add(timeLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(movesLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(ratingLabel);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        bottomPanel.setBackground(new Color(240, 240, 240));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton newGameButton = new JButton("Новая игра");
        newGameButton.setFont(new Font("Arial", Font.BOLD, 16));
        newGameButton.setBackground(new Color(54, 190, 82));
        newGameButton.setForeground(Color.WHITE);
        newGameButton.setFocusPainted(false);
        newGameButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        JButton exitButton = new JButton("Выход");
        exitButton.setFont(new Font("Arial", Font.BOLD, 16));
        exitButton.setBackground(new Color(200, 70, 70));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        newGameButton.addActionListener(e -> {
            dialog.dispose();
            restartGame();
        });

        exitButton.addActionListener(e -> {
            dialog.dispose();
            System.exit(0);
        });

        bottomPanel.add(newGameButton);
        bottomPanel.add(exitButton);

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void restartGame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            window.dispose();
        }

        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}