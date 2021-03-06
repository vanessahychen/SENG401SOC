/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007-2010 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.client;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.Enumeration;
import java.util.Timer;


/**
 * This is a component that can display a Settlers of Catan Board.
 * It can be used in an applet or an application.
 * It loads gifs from a directory named "images" in the same
 * directory as this class.
 *<P>
 * When the mouse is over the game board, a tooltip shows information
 * such as a hex's resource, a piece's owner, a port's ratio, or the
 * number under the robber. See {@link #hoverTip}.
 *<P>
 * During game play, moving the mouse over the board shows ghosted roads,
 * settlements, cities at locations the player can build.  See: {@link #hilight},
 * {@link SOCBoardPanel.BoardToolTip#hoverRoadID}.
 * Right-click to build, or use the {@link SOCBuildingPanel}'s buttons.
 *<P>
 * Before the game begins, the boardpanel is full of water hexes.
 * You can show a short message text of 1 or 2 lines by calling
 * {@link #setSuperimposedText(String, String)}.
 *<P>
 * During game play, you can show a short 1-line message text in the
 * top-center part of the panel by calling {@link #setSuperimposedTopText(String)}.
 */
public class SOCBoardPanel extends Canvas implements MouseListener, MouseMotionListener
{
    private static String IMAGEDIR = "/soc/client/images";

    /**
     * size of the whole panel, internal-pixels "scale".
     * This constant may not reflect the current game board's minimum size:
     * In board-internal coordinates, use {@link #panelMinBW} and {@link #panelMinBH} instead.
     * For mimimum acceptable size in on-screen pixels,
     * call {@link #getMinimumSize()} instead of using PANELX and PANELY directly.
     * For actual current size in screen pixels, see
     * {@link #scaledPanelX} {@link #scaledPanelY};
     * If {@link #isRotated()}, the minimum size swaps {@link #PANELX} and {@link #PANELY}.
     * If 6-player board, the minimum size is larger.
     */
    public static final int PANELX = 379, PANELY = 340;

    /** How many pixels to drop for each row of hexes. @see #HEXHEIGHT */
    private static final int deltaY = 46;
    /** How many pixels to move over for a new hex. @see #HEXWIDTH */
    private static final int deltaX = 54;
    /** Each row only moves a half hex over horizontally. @see #deltaX */
    private static final int halfdeltaX = 27;

    /**
     * x-offset to move over 1 hex, for each port facing direction (1-6). 0 is unused.
     * Facing is the direction to the land hex touching the port.
     * Facing 2 is E, 3 is SE, 4 is SW, etc: see {@link #hexLayout}.
     * @see #DELTAY_FACING
     * @since 1.1.08
     */
    private static final int[] DELTAX_FACING =
    {
        0, halfdeltaX, deltaX, halfdeltaX, -halfdeltaX, -deltaX, -halfdeltaX 
    };

    /**
     * y-offset to move over 1 hex, for each port facing direction (1-6). 0 is unused.
     * @see #DELTAX_FACING
     * @since 1.1.08
     */
    private static final int[] DELTAY_FACING =
    {
        0, -deltaY, 0, deltaY, deltaY, 0, -deltaY
    };

    /**
     * hex coordinates for drawing the standard board.
     * These were called hexX, hexY before 1.1.08.
     * @see #hexX_6pl
     */
    private static final int[] hexX_st =
    {
        deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX,  // row 1 4 hexes
        deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX,                                          // row 2 5 hexes
        halfdeltaX, deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX, 5 * deltaX + halfdeltaX,  // row 3 6 hexes
        0, deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX, 6 * deltaX,                           // row 4 7 hexes
        halfdeltaX, deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX, 5 * deltaX + halfdeltaX,  // row 5 6 hexes
        deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX,                                          // row 6 5 hexes
        deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX   // row 7 4 hexes
    };
    private static final int[] hexY_st =
    {
        0, 0, 0, 0, 
        deltaY, deltaY, deltaY, deltaY, deltaY, 
        2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 
        3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY,
        4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 
        5 * deltaY, 5 * deltaY, 5 * deltaY, 5 * deltaY, 5 * deltaY,
        6 * deltaY, 6 * deltaY, 6 * deltaY, 6 * deltaY
    };

    /**
     * hex coordinates for drawing the 6-player board, or null.
     * Initialized by constructor of the first 6-player boardpanel.
     * To make room for the ring of ports/water which isn't in the
     * coordinate system, the offset from {@link #hexX_st} is {@link #HEXX_OFF_6PL},
     * and the offset from {@link #hexY_st} is {@link #HEXY_OFF_6PL}.
     * @see #hexX
     * @since 1.1.08
     */
    private static int[] hexX_6pl, hexY_6pl;

    /**
     * In 6-player mode, the offset of {@link #hexX_6pl}, {@link #hexY_6pl}
     * from {@link #hexX_st}, {@link #hexY_st} in unscaled board coordinates.
     * Remember that x and y are swapped on-screen, because the board is rotated.
     * @since 1.1.08
     */
    private static final int HEXX_OFF_6PL = deltaX, HEXY_OFF_6PL = deltaY;

    /**
     * In 6-player mode, subtract this instead of {@link #HEXY_OFF_6PL}
     * in {@link #findEdge(int, int)}, {@link #findHex(int, int)}, {@link #findNode(int, int)}.
     * @since 1.1.08
     */
    private static final int HEXY_OFF_6PL_FIND = 7;

    /**
     * coordinates for drawing the playing pieces
     */
    /***  road looks like "|" along left edge of hex ***/
    private static final int[] vertRoadX = { -2, 3, 3, -2, -2 };
    private static final int[] vertRoadY = { 17, 17, 47, 47, 17 };

    /***  road looks like "/" along upper-left edge of hex ***/
    private static final int[] upRoadX = { -1, 26, 29, 2, -1 };
    private static final int[] upRoadY = { 15, -2, 2, 19, 15 };

    /***  road looks like "\" along lower-left edge of hex ***/
    private static final int[] downRoadX = { -1, 2, 29, 26, -1 };
    private static final int[] downRoadY = { 49, 45, 62, 66, 49 };

    /***  settlement  ***/
    private static final int[] settlementX = { -7, 0, 7, 7, -7, -7, 7 };
    private static final int[] settlementY = { -7, -14, -7, 5, 5, -7, -7 };

    /***  city  ***/
    private static final int[] cityX = 
    {
        -10, -4, 2, 2, 10, 10, -10, -10, 0, 0, 10, 5, -10
    };
    private static final int[] cityY = 
    {
        -8, -14, -8, -4, -4, 6, 6, -8, -8, -4, -4, -8, -8
    };

    /** robber polygon. X is -4 to +4; Y is -8 to +8. */
    private static final int[] robberX =
    {
        -2, -4, -4, -2, 2, 4, 4, 2, 4, 4, -4, -4, -2, 2
    };
    private static final int[] robberY =
    {
        -2, -4, -6, -8, -8, -6, -4, -2, 0, 8, 8, 0, -2, -2
    };

    /**
     * Arrow, left-pointing.
     * First point is top of arrow-tip's bevel, last point is bottom of tip.
     * arrowXL[4] is rightmost X coordinate.
     * (These points are important for adjustment when scaling in {@link #rescaleCoordinateArrays()})
     * @see #arrowY
     * @see #ARROW_SZ
     */
    private static final int[] arrowXL =
    {
        0,  17, 18, 18, 36, 36, 18, 18, 17,  0
    };
    /**
     * Arrow, right-pointing.
     * Calculated when needed by flipping {@link #arrowXL} in {@link #rescaleCoordinateArrays()}.
     */
    private static int[] arrowXR = null;
    /**
     * Arrow, y-coordinates: same whether pointing left or right.
     * First point is top of arrow-tip's bevel, last point is bottom of tip.
     */
    private static final int[] arrowY =
    {
        17,  0,  0,  6,  6, 30, 30, 36, 36, 19
    };

    /** Arrow fits in a 37 x 37 square. @see #arrowXL */
    private static final int ARROW_SZ = 37;

    /** Arrow color: cyan: r=106,g=183,b=183 */
    private static final Color ARROW_COLOR = new Color(106, 183, 183);

    /**
     * Arrow color when game is over,
     * and during {@link SOCGame#SPECIAL_BUILDING} phase of the 6-player game.
     *<P>
     * The game-over color was added in 1.1.09.  Previously, {@link #ARROW_COLOR} was used.
     * @since 1.1.08
     */
    private static final Color ARROW_COLOR_PLACING = new Color(255, 255, 60);

    /**
     * BoardPanel's {@link #mode}s. NONE is normal gameplay, or not the client's turn.
     * For correlation to game state, see {@link #updateMode()}.
     * If a mode is added, please also update {@link #clearModeAndHilight(int)}.
     */
    public final static int NONE = 0;
    public final static int PLACE_ROAD = 1;
    public final static int PLACE_SETTLEMENT = 2;
    public final static int PLACE_CITY = 3;
    public final static int PLACE_ROBBER = 4;
    public final static int PLACE_INIT_SETTLEMENT = 5;
    public final static int PLACE_INIT_ROAD = 6;
    public final static int CONSIDER_LM_SETTLEMENT = 7;
    public final static int CONSIDER_LM_ROAD = 8;
    public final static int CONSIDER_LM_CITY = 9;
    public final static int CONSIDER_LT_SETTLEMENT = 10;
    public final static int CONSIDER_LT_ROAD = 11;
    public final static int CONSIDER_LT_CITY = 12;
    public final static int TURN_STARTING = 97;
    public final static int GAME_FORMING = 98;
    public final static int GAME_OVER = 99;
    
    /** During initial-piece placement, the tooltip is moved this far over to make room. */
    public final static int HOVER_OFFSET_X_FOR_INIT_PLACE = 9;
    
    /** During robber placement, the tooltip is moved this far over to make room. */
    public final static int HOVER_OFFSET_X_FOR_ROBBER = 15;
    
    /** for popup-menu build request, network send maximum delay (seconds) */
    protected static int BUILD_REQUEST_MAX_DELAY_SEC = 5;
    
    /** for popup-menu build request, length of time after popup to ignore further
     *  mouse-clicks.  Avoids Windows accidental build by popup-click during game's
     *  initial piece placement. (150 ms)
     */ 
    protected static int POPUP_MENU_IGNORE_MS = 150;

    /**
     * Pixel spacing around {@link #superText1}, {@link #superText2}, {@link #superTextTop}
     * @since 1.1.07
     */
    private static final int SUPERTEXT_INSET = 3, SUPERTEXT_PADDING_HORIZ = 2 * SUPERTEXT_INSET + 2;

    /**
     * hex size, in unscaled internal-pixels: 55 wide, 64 tall.
     * The road polygon coordinate-arrays ({@link #downRoadX}, etc)
     * are plotted against a hex of this size.
     * @see #deltaX
     * @see #deltaY
     */
    private static final int HEXWIDTH = 55, HEXHEIGHT = 64;

    /**
     * Diameter and font size (unscaled internal-pixels) for dice number circles on hexes.
     * @since 1.1.08
     */
    private static final int DICE_NUMBER_CIRCLE_DIAMETER = 19, DICE_NUMBER_FONTPOINTS = 12;

    /**
     * Dice number circle background colors on hexes. <PRE>
     * Index:  Dice:  Color:
     *   0     2, 12  yellow
     *   1     3, 11
     *   2     4, 10  orange
     *   3     5,  9
     *   4     6,  8  red </PRE>
     *
     * @since 1.1.08
     */
    private static final Color[] DICE_NUMBER_CIRCLE_COLORS =
        { Color.YELLOW,
          new Color(255, 189, 0),
          new Color(255, 125, 0),
          new Color(255,  84, 0),
          Color.RED
        };

    /**
     * Minimum required width and height, as determined by options and {@link #isRotated}.
     * Used by {@link #getMinimumSize()}.
     * @since 1.1.08
     */
    private Dimension minSize;

    /**
     * Ensure that super.setSize is called at least once.
     * @since 1.1.08
     */
    private boolean hasCalledSetSize;

    /**
     * The board is configured for 6-player mode (and is {@link #isRotated});
     * set in constructor by checking {@link SOCBoard#getBoardEncodingFormat()}.
     * The entire coordinate system is land, except the rightmost hexes are unused
     * (7D-DD-D7 row).
     * The 6-player mode uses {@link #hexX_6pl} instead of {@link #hexX_st} for coordinates.
     * @see #inactiveHexNums
     * @since 1.1.08
     */
    protected boolean is6player;

    /**
     * The board is visually rotated 90 degrees clockwise (6-player: game opt PL > 4)
     * compared to the internal coordinates.
     *<P>
     * Use this for rotation:
     *<UL>
     *<LI> From internal to screen (cw):  P'=({@link #panelMinBH}-y, x)
     *<LI> From screen to internal (ccw): P'=(y, {@link #panelMinBW}-x)
     *</UL>
     * When the board is also {@link #isScaled scaled}, go in this order:
     * Rotate clockwise, then scale up; Scale down, then rotate counterclockwise.
     *<P>
     * When calculating position at which to draw an image or polygon,
     * remember that rotation changes which corner is considered (0,0),
     * and the image is offset from that corner.  (For example, {@link #drawHex(Graphics, int)}
     * subtracts HEXHEIGHT from x, after rotation but before scaling.)
     *
     * @see #isScaledOrRotated
     * @since 1.1.08
     */
    protected boolean isRotated;

    /**
     * Convenience flag - The board is {@link #isRotated rotated} and/or {@link #isScaled scaled up}.
     * Check both of those flags when transforming between screen coordinates
     * and internal coordinates.  Go in this order:
     * Rotate clockwise, then scale up; Scale down, then rotate counterclockwise.
     * @since 1.1.08
     */
    protected boolean isScaledOrRotated;

    /**
     * actual size on-screen, not internal-pixels size
     * ({@link #panelMinBW}, {@link #panelMinBH})
     */
    protected int scaledPanelX, scaledPanelY;

    /**
     * Minimum width and height, in board-internal coordinates.
     * Differs from static {@link #PANELX}, {@link #PANELY} for {@link #is6player 6-player board}.
     * Differs from {@link #minSize} because minSize takes {@link #isRotated} into account,
     * so minSize isn't suitable for use in rescaling formulas.
     * @since 1.1.08
     */
    protected final int panelMinBW, panelMinBH;

    /**
     * The board is currently scaled larger than
     * {@link #panelMinBW} x {@link #panelMinBH} pixels.
     * Use {@link #scaleToActualX(int)}, {@link #scaleFromActualX(int)},
     * etc to convert between internal and actual screen pixel coordinates.
     *<P>
     * When the board is also {@link #isRotated rotated}, go in this order:
     * Rotate clockwise, then scale up; Scale down, then rotate counterclockwise.
     *
     * @see #isScaledOrRotated
     */
    protected boolean isScaled;

    /**
     * Time of last resize, as returned by {@link System#currentTimeMillis()}.
     * Used with {@link #scaledMissedImage}.
     */
    protected long scaledAt;

    /**
     * Flag used while drawing a scaled board. If board size
     * was recently changed, could be waiting for an image to resize.
     * If it still hasn't appeared after 7 seconds, we'll give
     * up and create a new one.  (This can happen due to AWT bugs.)
     * Set in {@link #drawHex(Graphics, int)}, checked in {@link #drawBoard(Graphics)}.
     * @see #scaledHexFail
     */
    protected boolean scaledMissedImage;

    /**
     * Time of start of board repaint, as returned by {@link System#currentTimeMillis()}.
     * Used in {@link #drawBoardEmpty(Graphics)} with {@link #scaledMissedImage}.
     * @since 1.1.08
     */
    private long drawnEmptyAt;

    /**
     * Font of dice-number circles appearing on hexes.
     * @since 1.1.08
     */
    private Font diceNumberCircleFont;

    /**
     * FontMetrics of {@link #diceNumberCircleFont}.
     * @since 1.1.08
     */
    private FontMetrics diceNumberCircleFM;

    /**
     * translate hex ID to number to get coords
     */
    private int[] hexIDtoNum;

    /**
     * Hex numbers which aren't drawn, or null.
     * For {@link #is6player 6-player board},
     * the rightmost line of hexes (7D-DD-D7) are skipped.
     * Indicate this to {@link #drawBoard(Graphics)}.
     * @since 1.1.08
     */
    private boolean[] inactiveHexNums;

    /**
     * hex coordinates for drawing pieces on the board.
     * Points to {@link #hexX_st} or {@link #hexX_6pl},
     * and {@link #hexY_st} or {@link #hexY_6pl},
     * depending on {@link #is6player} flag.
     * @since 1.1.08
     */
    private int[] hexX, hexY;

    /**
     * Hex pix - shared unscaled original-resolution from GIF files.
     * Note that miscPort0.gif - miscPort5.gif are stored in {@link #hexes};
     * {@link #ports} stores the resource ports.
     * @see #scaledHexes
     * @see #rotatHexes
     */
    private static Image[] hexes, ports;

    /**
     * Hex pix - rotated board; from ./images/rotat GIF files.
     * Image references are copied to
     * {@link #scaledHexes}/{@link #scaledPorts} from here.
     * @see #hexes
     * @since 1.1.08
     */
    private static Image[] rotatHexes, rotatPorts;

    /**
     * Hex pix - private scaled copy, if isScaled. Otherwise points to static copies,
     * either {@link #hexes} or {@link #rotatHexes}
     */
    private Image[] scaledHexes, scaledPorts;

    /**
     * Hex pix - Flag to check if rescaling failed, if isScaled.
     * @see #scaledHexes
     * @see #drawHex(Graphics, int)
     */
    private boolean[] scaledHexFail, scaledPortFail;

    /**
     * dice number pix (for arrow). @see #DICE_SZ
     */
    private static Image[] dice;

    /** Dice number graphic fits in a 25 x 25 square. @see #dice */
    private static final int DICE_SZ = 25;

    /**
     * Coordinate arrays for drawing the playing pieces.
     * Local copy if isScaled, otherwise points to static arrays.
     */
    private int[] scaledVertRoadX, scaledVertRoadY;

    /***  road looks like "/"  ***/
    private int[] scaledUpRoadX, scaledUpRoadY;

    /***  road looks like "\"  ***/
    private int[] scaledDownRoadX, scaledDownRoadY;

    /***  settlement  ***/
    private int[] scaledSettlementX, scaledSettlementY;

    /***  city  ***/
    private int[] scaledCityX, scaledCityY;

    /***  robber  ***/
    private int[] scaledRobberX, scaledRobberY;

    /** 
     * arrow, left-pointing and right-pointing.
     * @see #rescaleCoordinateArrays()
     */
    private int[] scaledArrowXL, scaledArrowXR, scaledArrowY; 

    /** hex corners, clockwise from top-center, as located in waterHex.gif and other hex graphics:
     * (27,0) (54,16) (54,47) (27,63) (0,47) (0,16).
     *  If rotated 90deg clockwise, clockwise from center-right, would be:
     * (63,27) (47,54) (16,54) (0,27) (16,0) (47,0).
     * @see #hexCornersY
     * @since 1.1.07
     */
    private static final int[] hexCornersX =
    {
    	27, 54, 54, 27, 0, 0
    };

    /** hex corners, clockwise from top-center.
     * @see #hexCornersX
     * @since 1.1.07
     */
    private static final int[] hexCornersY =
    {
    	0, 16, 47, 63, 47, 16
    };

    /** 
     * hex corner coordinates, as scaled to current board size.
     * @see #hexCornersX
     * @see #rescaleCoordinateArrays()
     * @since 1.1.07
     */
    private int[] scaledHexCornersX, scaledHexCornersY; 

    /**
     * Old pointer coords for interface
     */
    private int ptrOldX, ptrOldY;
    
    /**
     * (tooltip) Hover text.  Its mode uses boardpanel mode
     * constants: Will be NONE, PLACE_ROAD, PLACE_SETTLEMENT,
     *   PLACE_ROBBER for hex, or PLACE_INIT_SETTLEMENT for port.
     */
    private BoardToolTip hoverTip;

    /**
     * Context menu for build/cancel-build
     */
    private BoardPopupMenu popupMenu;

    /**
     * Tracks last menu-popup time.  Avoids misinterpretation of popup-click with placement-click
     * during initial placement: On Windows, popup-click must be caught in mouseReleased,
     * but mousePressed is called immediately afterwards.    
     */
    private long popupMenuSystime;

    /**
     * For right-click build menu; used for fallback part of client-server-client
     * communication of a build request. Created whenever right-click build request sent.
     * This is the fallback for the normal method:
     * <pre>
     *  SOCBoardPanel.popupExpectingBuildRequest
     *  SOCPlayerInterface.updateAtGameState
     *  SOCBoardPanel.popupFireBuildingRequest
     * </pre>
     */
    protected BoardPanelSendBuildTask buildReqTimerTask;

    /**
     * Text to be displayed as 2 lines superimposed over center
     * of the board graphic (during game setup).
     * Either supertext2, or both, can be null to display nothing.
     * @see #setSuperimposedText(String, String)
     * @since 1.1.07
     */
    private String superText1, superText2;

    /**
     * Width, height of {@link #superText1} and {@link #superText2} if known, or 0.
     * Calculated in {@link #drawSuperText(Graphics)}.
     * @since 1.1.07
     */
    private int superText1_w, superText_h, superText_des, superText2_w, superTextBox_x, superTextBox_y, superTextBox_w, superTextBox_h;

    /**
     * Text to be displayed as 1 line superimposed over the top-center
     * of the board graphic (during game play).
     * Can be null to display nothing.
     * @see #setSuperimposedTopText(String)
     * @since 1.1.08
     */
    private String superTextTop;

    /**
     * Width, height of {@link #superTextTop} if known, or 0.
     * Calculated in {@link #drawSuperTextTop(Graphics)}.
     * Y-position of top of this textbox is {@link #SUPERTEXT_INSET}.
     * @since 1.1.08
     */
    private int superTextTop_w, superTextTop_h, superTextTopBox_x, superTextTopBox_w, superTextTopBox_h;

    /**
     * Edge or node being pointed to. When placing a road/settlement/city,
     * used for coordinate of "ghost" piece under the mouse pointer.
     * 0 when nothing is hilighted. -1 for a road at edge 0x00.
     */
    private int hilight;

    /**
     * Map grid sectors (from unscaled on-screen coordinates) to hex edges.
     * The grid has 15 columns and 23 rows.
     * This maps graphical coordinates to the board coordinate system.
     *<P>
     * <b>Note:</b> For the 6-player board, edge 0x00 is a valid edge that
     * can be built on.  It is marked here as -1, since a value of 0 marks an
     * invalid edge in this map.
     *<P>
     * The edge number at grid (x,y) is in edgeMap[x + (y * 15)].
     *<P>
     * In {@link #is6player 6-player mode}, there is an extra ring of water/port hexes
     * on the outside, which isn't within the coordinate system.  So this grid is
     * shifted +1 column, +3 rows.
     *
     * @see #findEdge(int, int)
     * @see #initEdgeMapAux(int, int, int, int, int)
     */
    private int[] edgeMap;

    /**
     * Map grid sectors (from unscaled on-screen coordinates) to hex nodes.
     * The grid has 15 columns and 23 rows.
     * This maps graphical coordinates to the board coordinate system.
     * Each row of hexes touches 3 columns and 5 rows here. For instance,
     * hex 0x35 has its top-center point (node) in row y=6, and its bottom-center
     * point in row y=10, of this grid.  Its left edge is column x=3, and right is column x=5.
     *<P>
     * The node number at grid (x,y) is nodeMap[x + (y * 15)].
     *<P>
     * In {@link #is6player 6-player mode}, there is an extra ring of water/port hexes
     * on the outside, which isn't within the coordinate system.  So this grid is
     * shifted +1 column, +3 rows.
     *
     * @see #findNode(int, int)
     * @see #initNodeMapAux(int, int, int, int, int)
     */
    private int[] nodeMap;

    /**
     * Map grid sectors (from on-screen coordinates) to hexes.
     * @see #findHex(int, int)
     */
    private int[] hexMap;

    /**
     * The game which this board is a part of
     */
    private SOCGame game;

    /**
     * The board in the game
     */
    private SOCBoard board;

    /**
     * The player that is using this interface
     */
    private SOCPlayer player;
    
    /**
     * player number if in a game, or -1.
     */
    private int playerNumber;

    /**
     * When in "consider" mode, this is the player
     * we're talking to
     */
    private SOCPlayer otherPlayer;

    /**
     * offscreen buffer of everything (board, pieces, hovering pieces, tooltip), to prevent flicker.
     * @see #emptyBoardBuffer
     */
    private Image buffer;

    /**
     * offscreen buffer of board without any pieces placed, to prevent flicker.
     * If the board layout changes (at start of game, for example),
     * call {@link #flushBoardLayoutAndRepaint()} to clear the buffered copy.
     * @see #buffer
     * @since 1.1.08
     */
    private Image emptyBoardBuffer;

    /**
     * Modes of interaction; for correlation to game state, see {@link #updateMode()}.
     * For tooltip's mode, see {@link SOCBoardPanel.BoardToolTip#hoverMode}.
     */
    private int mode;

    /**
     * This holds the coord of the last stlmt
     * placed in the initial phase.
     */
    private int initstlmt;

    /**
     * the player interface that this board is a part of
     */
    private SOCPlayerInterface playerInterface;

    /** Cached colors, for use for robber's "ghost"
     *  (previous position) when moving the robber.
     *  Values are determined the first time the
     *  robber is ghosted on that type of tile.
     *  
     *  Index ranges from 0 to SOCBoard.MAX_ROBBER_HEX.
     *  
     *  @see soc.client.ColorSquare
     *  @see #drawRobber(Graphics, int, boolean)
     */
    protected Color[] robberGhostFill, robberGhostOutline;

    /**
     * create a new board panel in a game interface.
     * The minimum size needed on-screen is based on the game options.
     * After construction, call {@link #getMinimumSize()} to read it.
     *
     * @param pi  the player interface that spawned us
     */
    public SOCBoardPanel(SOCPlayerInterface pi)
    {
        super();

        game = pi.getGame();
        playerInterface = pi;
        player = null;
        playerNumber = -1;
        board = game.getBoard();
        isScaled = false;
        scaledMissedImage = false;
        is6player = (board.getBoardEncodingFormat() == SOCBoard.BOARD_ENCODING_6PLAYER)
            || (game.maxPlayers > 4);
        isRotated = isScaledOrRotated = is6player;
        if (isRotated)
        {
            // scaledPanelX, scaledPanelY are on-screen minimum size.
            // panelMinBW, panelMinBH are board-coordinates, so not rotated.
            // Thus, x <-> y between these two pairs of variables.
            scaledPanelX = PANELY;
            scaledPanelY = PANELX;
            if (is6player)
            {
                scaledPanelX += (2 * deltaY);
                scaledPanelY += deltaX;
            }
            panelMinBW = scaledPanelY;
            panelMinBH = scaledPanelX;
        } else {
            panelMinBW = scaledPanelX = PANELX;
            panelMinBH = scaledPanelY = PANELY;
            if (is6player)  // JM TODO: testing only, normally isRotated
            {
                scaledPanelY += (2 * deltaY);
                scaledPanelX += deltaX;
            }
        }
        minSize = new Dimension(scaledPanelX, scaledPanelY);
        hasCalledSetSize = false;

        int i;

        // init coord holders
        ptrOldX = 0;
        ptrOldY = 0;

        hilight = 0;

        // init edge map
        edgeMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            edgeMap[i] = 0;
        }

        if (is6player)
        {
            // since 0x00 is a valid edge for 6player, it's
            // marked in the map as -1 (0 means invalid in the map).
            initEdgeMapAux(3, 0, 9, 3, 0x17);    // Top row: 0x17 is first land hex of this row
            initEdgeMapAux(2, 3, 10, 6, 0x15);
            initEdgeMapAux(1, 6, 11, 9, 0x13);
            initEdgeMapAux(0, 9, 12, 12, 0x11);  // Middle row: 0x11 is leftmost land hex
            initEdgeMapAux(1, 12, 11, 15, 0x31);
            initEdgeMapAux(2, 15, 10, 18, 0x51);
            initEdgeMapAux(3, 18, 9, 21, 0x71);  // Bottom row: 0x71 is first land hex of this row
        } else {
            initEdgeMapAux(4, 3, 10, 6, 0x37);    // Top row: 0x37 is first land hex of this row
            initEdgeMapAux(3, 6, 11, 9, 0x35);
            initEdgeMapAux(2, 9, 12, 12, 0x33);  // Middle row: 0x33 is leftmost land hex
            initEdgeMapAux(3, 12, 11, 15, 0x53);
            initEdgeMapAux(4, 15, 10, 18, 0x73);  // Bottom row: 0x73 is first land hex of this row
        }

        // init node map
        nodeMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            nodeMap[i] = 0;
        }

        if (is6player)
        {
            initNodeMapAux(3,  0,  9,  4, 0x17);  // Very top row: 3 across
            initNodeMapAux(2,  3, 10,  7, 0x15);
            initNodeMapAux(1,  6, 11, 10, 0x13);
            initNodeMapAux(0,  9, 12, 13, 0x11);  // Middle row: 6 across, 0x11 is leftmost land hex
            initNodeMapAux(1, 12, 11, 16, 0x31);
            initNodeMapAux(2, 15, 10, 19, 0x51);
            initNodeMapAux(3, 18,  9, 22, 0x71);  // Very bottom row: 3 across
        } else {
            initNodeMapAux(4,  3, 10,  7, 0x37);  // Top row: 0x37 is first land hex of this row
            initNodeMapAux(3,  6, 11, 10, 0x35);
            initNodeMapAux(2,  9, 12, 13, 0x33);  // Middle row: 0x33 is leftmost land hex
            initNodeMapAux(3, 12, 11, 16, 0x53);
            initNodeMapAux(4, 15, 10, 19, 0x73);  // Bottom row: 0x73 is first land hex of this row
        }

        // init hex map
        hexMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            hexMap[i] = 0;
        }

        if (is6player)
        {
            initHexMapAux(3, 1, 8, 2, 0x17);    // Top row: 0x17 is first land hex
            initHexMapAux(2, 4, 9, 5, 0x15);
            initHexMapAux(1, 7, 10, 8, 0x13);
            initHexMapAux(0, 10, 11, 11, 0x11);
            initHexMapAux(1, 13, 10, 14, 0x31);
            initHexMapAux(2, 16, 9, 17, 0x51);
            initHexMapAux(3, 19, 8, 20, 0x71);  // Bottom row: 0x71 is first land hex
        } else {
            initHexMapAux(4, 4, 9, 5, 0x37);    // Top row: 0x37 is first land hex
            initHexMapAux(3, 7, 10, 8, 0x35);
            initHexMapAux(2, 10, 11, 11, 0x33);
            initHexMapAux(3, 13, 10, 14, 0x53);
            initHexMapAux(4, 16, 9, 17, 0x73);  // Bottom row: 0x73 is first land hex
        }

        hexIDtoNum = new int[0xDE];

        for (i = 0; i < 0xDE; i++)
        {
            hexIDtoNum[i] = 0;
        }

        initHexIDtoNumAux(0x17, 0x7D, 0);
        initHexIDtoNumAux(0x15, 0x9D, 4);
        initHexIDtoNumAux(0x13, 0xBD, 9);
        initHexIDtoNumAux(0x11, 0xDD, 15);
        initHexIDtoNumAux(0x31, 0xDB, 22);
        initHexIDtoNumAux(0x51, 0xD9, 28);
        initHexIDtoNumAux(0x71, 0xD7, 33);

        if (is6player)
        {
            if (hexX_6pl == null)
            {
                final int L = hexX_st.length;
                hexX_6pl = new int[L];
                hexY_6pl = new int[L];
                for (i = 0; i < L; ++i)
                    hexX_6pl[i] = hexX_st[i] + HEXX_OFF_6PL;
                for (i = 0; i < L; ++i)
                    hexY_6pl[i] = hexY_st[i] + HEXY_OFF_6PL;
            }
            hexX = hexX_6pl;
            hexY = hexY_6pl;

            // Hex numbers (in range 0-36) to skip: (coords 7D-DD-D7).
            inactiveHexNums = new boolean[hexX_6pl.length];
            int[] inacIdx = {3, 8, 14, 21, 27, 32, 36};
            for (i = 0; i < inacIdx.length; ++i)
                inactiveHexNums[inacIdx[i]] = true;
        } else {
            hexX = hexX_st;
            hexY = hexY_st;
            inactiveHexNums = null;
        }

        // set mode of interaction
        mode = NONE;

        // Set up mouse listeners
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        
        // Cached colors to be determined later
        robberGhostFill = new Color [1 + SOCBoard.MAX_ROBBER_HEX];
        robberGhostOutline = new Color [1 + SOCBoard.MAX_ROBBER_HEX];
        
        // Set up hover tooltip info
        hoverTip = new BoardToolTip(this);
        
        // Set up popup menu
        popupMenu = new BoardPopupMenu(this);
        add (popupMenu);
        popupMenuSystime = System.currentTimeMillis();  // Set to a reasonable value 

        // Overlay text
        superText1 = null;
        superText2 = null;

        // load the static images
        loadImages(this, isRotated);

        // point to static images, unless we're later resized
        scaledHexes = new Image[hexes.length];
        scaledPorts = new Image[ports.length];
        Image[] h, p;
        if (isRotated)
        {
            h = rotatHexes;
            p = rotatPorts;
        } else {
            h = hexes;
            p = ports;
        }
        for (i = hexes.length - 1; i>=0; --i)
            scaledHexes[i] = h[i];
        for (i = ports.length - 1; i>=0; --i)
            scaledPorts[i] = p[i];
        scaledHexFail = new boolean[hexes.length];
        scaledPortFail = new boolean[ports.length];

        // point to static coordinate arrays, unless we're later resized.
        // If this is the first instance, calculate arrowXR.
        rescaleCoordinateArrays();
    }

    private final void initEdgeMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int x;
        int y;
        int facing = 0;
        int count = 0;
        int hexNum;
        int edgeNum = 0;

        for (y = y1; y <= y2; y++)
        {
            hexNum = startHex;

            switch (count)
            {
            case 0:
                facing = 6;
                edgeNum = hexNum - 0x10;

                break;

            case 1:
                facing = 5;
                edgeNum = hexNum - 0x11;

                break;

            case 2:
                facing = 5;
                edgeNum = hexNum - 0x11;

                break;

            case 3:
                facing = 4;
                edgeNum = hexNum - 0x01;

                break;

            default:
                System.out.println("initEdgeMap error");

                return;
            }

            if (edgeNum == 0x00)
                edgeNum = -1;  // valid edge 0x00 is stored as -1 in map

            for (x = x1; x <= x2; x++)
            {
                edgeMap[x + (y * 15)] = edgeNum;

                switch (facing)
                {
                case 1:
                    facing = 6;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x10;

                    break;

                case 2:
                    facing = 5;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x11;

                    break;

                case 3:
                    facing = 4;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x01;

                    break;

                case 4:
                    facing = 3;
                    edgeNum = hexNum + 0x10;

                    break;

                case 5:
                    facing = 2;
                    edgeNum = hexNum + 0x11;

                    break;

                case 6:
                    facing = 1;
                    edgeNum = hexNum + 0x01;

                    break;

                default:
                    System.out.println("initEdgeMap error");

                    return;
                }
            }

            count++;
        }
    }

    private final void initHexMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int x;
        int y;
        int hexNum;
        int count = 0;

        for (y = y1; y <= y2; y++)
        {
            hexNum = startHex;

            for (x = x1; x <= x2; x++)
            {
                hexMap[x + (y * 15)] = hexNum;

                if ((count % 2) != 0)
                {
                    hexNum += 0x22;
                }

                count++;
            }
        }
    }

    /**
     * Within {@link #nodeMap}, set the node coordinates within a rectangular section
     * from (x1,y1) to (x2,y2) covering all nodes of one horizontal row of hexes.
     *<P>
     * The grid maps graphical coordinates to the board coordinate system.
     * Each row of hexes covers 5 rows here. For instance, hex 0x35 has its top-center
     * point (node) in row 6, and its bottom-center point in row 10.
     * All 6 nodes of each hex in range will be initialized within {@link #nodeMap}.
     *<P>
     * For node coordinates, see RST dissertation figure A2. For hex coordinates, see figure A1.
     *
     * @param x1 Starting x-coordinate within {@link #nodeMap}'s index;
     *           should correspond to left edge of <tt>startHex</tt>
     * @param y1 Starting y-coordinate within {@link #nodeMap}'s index;
     *           should correspond to top point of <tt>startHex</tt>
     * @param x2 Ending x-coordinate; should correspond to right edge of the last hex in the
     *           row of hexes being initialized.  Each hex is 2 units wide in the grid.
     * @param y2 Ending y-coordinate; should correspond to bottom point of <tt>startHex</tt>,
     *           and thus should be y1 + 4.
     * @param startHex  Starting hex ID (0x-coordinate of first hex in this row), to use with nodeMap[x1, y1].
     */
    private final void initNodeMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int rowState = 0;  // current state; related to row# and logic for node coords from hex coords
        int row = 0;  // 0 for first row (y==y1), 1 for second, etc.
        int hexNum;  // starts with startHex, incr by 0x22 to move across a horizontal row of board coords
                     // during rowStates 01, 12, 32, 41.
        int nodeNum = 0;  // node number

        /**
         * Brief Illustration of row, rowState, nodeNum:
         *   As seen for startHex = 0x37.  Node numbers below are in hex.
         *         x-- 4     5     6     7     8        4     5     6     7     8
         * row   y
         *  |    |     nodeNums: (0 where blank)       rowState at top of x-loop:
         *  |    |
         *  0    3     0     38          5A            00    01    00    01    00
         *                /      \    /      \    /       /      \    /      \    /
         *  1    4     27          49          6B      10    11    12    11    12
         *             |           |           |       |           |           |
         *  2    5     0           0           0       20    20    20    20    20
         *             |           |           |       |           |           |
         *  3    6     36          58          7A      30    31    32    31    32
         *           /    \      /    \      /    \       \      /    \      /    \
         *  4    7     0     47          69            40    41    40    41    40
         */

        for (int y = y1; y <= y2; y++, row++)
        {
            hexNum = startHex;

            switch (row)
            {
            case 0:
                rowState = 00;
                nodeNum = 0;

                break;

            case 1:
                rowState = 10;
                nodeNum = hexNum - 0x10;

                break;

            case 2:
                rowState = 20;
                nodeNum = 0;

                break;

            case 3:
                rowState = 30;
                nodeNum = hexNum - 0x01;

                break;

            case 4:
                rowState = 40;
                nodeNum = 0;

                break;

            default:
                System.out.println("initNodeMap error");

                return;
            }

            for (int x = x1; x <= x2; x++)
            {
                nodeMap[x + (y * 15)] = nodeNum;

                switch (rowState)
                {
                // Used in top row (row==0) //
                case 01:
                    rowState = 00;
                    hexNum += 0x22;
                    nodeNum = 0;

                    break;

                case 00:
                    rowState = 01;
                    nodeNum = hexNum + 0x01;

                    break;

                // Used in row 1 (row==1) //
                case 12:
                    rowState = 11;
                    hexNum += 0x22;
                    nodeNum = 0;

                    break;

                case 11:
                    rowState = 12;
                    nodeNum = hexNum + 0x12;

                    break;

                case 10:
                    rowState = 11;
                    nodeNum = 0;

                    break;

                // Used in middle row (row==2) //
                case 20:
                    nodeNum = 0;

                    break;

                // Used in row 3 //
                case 30:
                    rowState = 31;
                    nodeNum = 0;

                    break;

                case 32:
                    rowState = 31;
                    hexNum += 0x22;
                    nodeNum = 0;

                    break;

                case 31:
                    rowState = 32;
                    nodeNum = hexNum + 0x21;

                    break;

                // Used in bottom row (row==4) //
                case 41:
                    rowState = 40;
                    hexNum += 0x22;
                    nodeNum = 0;

                    break;

                case 40:
                    rowState = 41;
                    nodeNum = hexNum + 0x10;

                    break;

                default:
                    System.out.println("initNodeMap error");

                    return;
                }
            }  // for (x)
        }  // for (y)
    }

    private final void initHexIDtoNumAux(int begin, int end, int num)
    {
        int i;

        for (i = begin; i <= end; i += 0x22)
        {
            hexIDtoNum[i] = num;
            num++;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getPreferredSize()
    {
        return new Dimension(scaledPanelX, scaledPanelY);
    }

    /**
     * Minimum required width and height, as determined by options and {@link #isRotated()}.
     *
     * @return minimum size
     */
    public Dimension getMinimumSize()
    {
        return minSize;
    }

    /**
     * Set the board to a new size, rescale graphics and repaint if needed.
     *
     * @param newW New width in pixels, no less than {@link #getMinimumSize()}.width
     * @param newH New height in pixels, no less than {@link #getMinimumSize()}.height
     * @throws IllegalArgumentException if newW or newH is too small but not 0.
     *   During initial layout, the layoutmanager may make calls to setSize(0,0);
     *   such a call is passed to super without scaling graphics.
     */
    public void setSize(int newW, int newH)
        throws IllegalArgumentException
    {
        if ((newW == scaledPanelX) && (newH == scaledPanelY) && hasCalledSetSize)
            return;  // Already sized.

        // If below min-size, rescaleBoard throws
        // IllegalArgumentException. Pass to our caller.
        rescaleBoard(newW, newH);

        // Resize
        super.setSize(newW, newH);
        hasCalledSetSize = true;
        repaint();
    }

    /**
     * Set the board to a new size, rescale graphics and repaint if needed.
     *
     * @param sz New size in pixels, no less than {@link #panelMinBW} wide by {@link #panelMinBH} tall
     * @throws IllegalArgumentException if sz is too small but not 0.
     *   During initial layout, the layoutmanager may make calls to setSize(0,0);
     *   such a call is passed to super without scaling graphics.
     */
    public void setSize(Dimension sz)
        throws IllegalArgumentException
    {
        setSize (sz.width, sz.height);
    }

    /**
     * Set the board to a new location and size, rescale graphics and repaint if needed.
     * Called from {@link SOCPlayerInterface#doLayout()}.
     *
     * @param x New location's x-coordinate
     * @param y new location's y-coordinate
     * @param w New width in pixels, no less than {@link #PANELX} (or if rotated, {@link #PANELY}})
     * @param h New height in pixels, no less than {@link #PANELY} (or if rotated, {@link #PANELX})
     * @throws IllegalArgumentException if w or h is too small but not 0.
     *   During initial layout, the layoutmanager may make calls to setBounds(0,0,0,0);
     *   such a call is passed to super without scaling graphics.
     */
    public void setBounds(int x, int y, int w, int h)
        throws IllegalArgumentException
    {
        if ((w != scaledPanelX) || (h != scaledPanelY))            
        {
            rescaleBoard(w, h);
        }
        super.setBounds(x, y, w, h);
    }

    /**
     * Clear the board layout (as rendered in the
     * empty-board buffer) and trigger a repaint.
     * @since 1.1.08
     */
    public void flushBoardLayoutAndRepaint()
    {
        if (emptyBoardBuffer != null)
        {
            emptyBoardBuffer.flush();
            emptyBoardBuffer = null;
        }
        if (isScaled)
            scaledAt = System.currentTimeMillis();  // reset the image-scaling timeout 
        repaint();
    }

    /**
     * Set the board fields to a new size, rescale graphics if needed.
     * Does not call repaint.  Does not call setSize.
     * Will update {@link #isScaledOrRotated}, {@link #scaledPanelX}, and other fields.
     *
     * @param newW New width in pixels, no less than {@link #PANELX} (or if rotated, {@link #PANELY}})
     * @param newH New height in pixels, no less than {@link #PANELY} (or if rotated, {@link #PANELX})
     * @throws IllegalArgumentException if newW or newH is too small but not 0.
     *   During initial layout, the layoutmanager may cause calls to rescaleBoard(0,0);
     *   such a call is ignored, no rescaling of graphics is done.
     */
    private void rescaleBoard(final int newW, final int newH)
        throws IllegalArgumentException
    {
        if ((newW == 0) || (newH == 0))
            return;
        if ((newW < minSize.width) || (newH < minSize.height))
            throw new IllegalArgumentException("Below minimum size");

        /**
         * Set vars
         */
        scaledPanelX = newW;
        scaledPanelY = newH;
        isScaled = ((scaledPanelX != minSize.width) || (scaledPanelY != minSize.height));
        scaledAt = System.currentTimeMillis();
        isScaledOrRotated = (isScaled || isRotated);

        /**
         * Off-screen buffer is now the wrong size.
         * paint() will create a new one.
         */
        if (buffer != null)
        {
            buffer.flush();
            buffer = null;
        }
        if (emptyBoardBuffer != null)
        {
            emptyBoardBuffer.flush();
            emptyBoardBuffer = null;
        }
        diceNumberCircleFont = null;
        diceNumberCircleFM = null;

        /**
         * Scale coordinate arrays for drawing pieces,
         * or (if not isScaled) point to static arrays.
         */
        rescaleCoordinateArrays();

        /**
         * Scale images, or point to static arrays.
         */
        Image[] hex, por;
        if (isRotated)
        {
            hex = rotatHexes;
            por = rotatPorts;
        } else {
            hex = hexes;
            por = ports;
        }
        if (! isScaled)
        {
            int i;
            for (i = scaledHexes.length - 1; i>=0; --i)
                scaledHexes[i] = hex[i];
            for (i = ports.length - 1; i>=0; --i)
                scaledPorts[i] = por[i];
        }
        else
        {
            int w = scaleToActualX(hex[0].getWidth(null));
            int h = scaleToActualY(hex[0].getHeight(null));
            for (int i = scaledHexes.length - 1; i>=0; --i)
            {
                scaledHexes[i] = hex[i].getScaledInstance(w, h, Image.SCALE_SMOOTH);
                scaledHexFail[i] = false;
            }

            w = scaleToActualX(por[1].getWidth(null));
            h = scaleToActualY(por[1].getHeight(null));
            for (int i = scaledPorts.length - 1; i>=1; --i)
            {
                scaledPorts[i] = por[i].getScaledInstance(w, h, Image.SCALE_SMOOTH);
                scaledPortFail[i] = false;
            }
        }

        if ((superText1 != null) && (superTextBox_w > 0))
        {
            superTextBox_x = (newW - superTextBox_w) / 2;
            superTextBox_y = (newH - superTextBox_h) / 2;
        }
    }

    /**
     * Scale coordinate arrays for drawing pieces
     * (from internal coordinates to actual on-screen pixels),
     * or (if not isScaled) point to static arrays.
     * Called from constructor and rescaleBoard.
     */
    private void rescaleCoordinateArrays()
    {
        if (! isScaled)
        {
            if (! isRotated)
            {
                scaledVertRoadX = vertRoadX;     scaledVertRoadY = vertRoadY;
                scaledUpRoadX   = upRoadX;       scaledUpRoadY   = upRoadY;
                scaledDownRoadX = downRoadX;     scaledDownRoadY = downRoadY;
                scaledHexCornersX = hexCornersX; scaledHexCornersY = hexCornersY;
            } else {
                // (cw):  P'=(width-y, x)
                scaledVertRoadX = rotateScaleCopyYToActualX(vertRoadY, HEXWIDTH, false);
                scaledVertRoadY = vertRoadX;
                scaledUpRoadX   = rotateScaleCopyYToActualX(upRoadY, HEXWIDTH, false);
                scaledUpRoadY   = upRoadX;
                scaledDownRoadX = rotateScaleCopyYToActualX(downRoadY, HEXWIDTH, false);
                scaledDownRoadY = downRoadX;
                scaledHexCornersX = rotateScaleCopyYToActualX(hexCornersY, HEXWIDTH, false);
                scaledHexCornersY = hexCornersX;
            }
            scaledSettlementX = settlementX; scaledSettlementY = settlementY;
            scaledCityX     = cityX;         scaledCityY     = cityY;
            scaledRobberX   = robberX;       scaledRobberY   = robberY;
            scaledArrowXL   = arrowXL;       scaledArrowY    = arrowY;
            if (arrowXR == null)
            {
                int[] axr = new int[arrowXL.length];
                for (int i = 0; i < arrowXL.length; ++i)
                    axr[i] = (ARROW_SZ - 1) - arrowXL[i];
                arrowXR = axr;

                // Assigned to static field only when complete,
                // so another thread won't see a partially
                // calculated arrowXR.
            }
            scaledArrowXR = arrowXR;
        }
        else
        {
            if (! isRotated)
            {
                scaledVertRoadX = scaleCopyToActualX(vertRoadX);
                scaledVertRoadY = scaleCopyToActualY(vertRoadY);
                scaledUpRoadX   = scaleCopyToActualX(upRoadX);
                scaledUpRoadY   = scaleCopyToActualY(upRoadY);
                scaledDownRoadX = scaleCopyToActualX(downRoadX);
                scaledDownRoadY = scaleCopyToActualY(downRoadY);
                scaledHexCornersX = scaleCopyToActualX(hexCornersX);
                scaledHexCornersY = scaleCopyToActualY(hexCornersY);
            } else {
                // (cw):  P'=(width-y, x)
                scaledVertRoadX = rotateScaleCopyYToActualX(vertRoadY, HEXWIDTH, true);
                scaledVertRoadY = scaleCopyToActualY(vertRoadX);
                scaledUpRoadX   = rotateScaleCopyYToActualX(upRoadY, HEXWIDTH, true);
                scaledUpRoadY   = scaleCopyToActualY(upRoadX);
                scaledDownRoadX = rotateScaleCopyYToActualX(downRoadY, HEXWIDTH, true);
                scaledDownRoadY = scaleCopyToActualY(downRoadX);
                scaledHexCornersX = rotateScaleCopyYToActualX(hexCornersY, HEXWIDTH, true);
                scaledHexCornersY = scaleCopyToActualY(hexCornersX);
            }
            scaledSettlementX = scaleCopyToActualX(settlementX);
            scaledSettlementY = scaleCopyToActualY(settlementY);
            scaledCityX     = scaleCopyToActualX(cityX);
            scaledCityY     = scaleCopyToActualY(cityY);
            scaledRobberX   = scaleCopyToActualX(robberX);
            scaledRobberY   = scaleCopyToActualY(robberY);
            scaledArrowXL   = scaleCopyToActualX(arrowXL);
            scaledArrowY    = scaleCopyToActualY(arrowY);

            // Ensure arrow-tip sides are 45 degrees.
            int p = Math.abs(scaledArrowXL[0] - scaledArrowXL[1]);
            if (p != Math.abs(scaledArrowY[0] - scaledArrowY[1]))
            {
                scaledArrowY[0] = scaledArrowY[1] + p;
            }
            int L = scaledArrowXL.length - 1;
            p = Math.abs(scaledArrowXL[L] - scaledArrowXL[L-1]);
            if (p != Math.abs(scaledArrowY[L] - scaledArrowY[L-1]))
            {
                scaledArrowY[L] = scaledArrowY[L-1] - p;
            }

            // Now, flip for scaledArrowXR
            scaledArrowXR = new int[scaledArrowXL.length];
            int xmax = scaledArrowXL[4];  // Element defined as having max coord in arrowXL
            for (int i = 0; i < scaledArrowXR.length; ++i)
                scaledArrowXR[i] = xmax - scaledArrowXL[i];
        }
    }

    /**
     * Rescale to actual screen coordinates - Create a copy
     * of array, and scale the copy's elements as X coordinates.
     *
     * @param xorig Int array to be scaled; each member is an x-coordinate.
     * @return Scaled copy of xorig
     *
     * @see #scaleToActualX(int[])
     * @see #rotateScaleCopyYToActualX(int[], int, boolean)
     */
    public int[] scaleCopyToActualX(int[] xorig)
    {
        int[] xs = new int[xorig.length];
        for (int i = xorig.length - 1; i >= 0; --i)
            xs[i] = (int) ((xorig[i] * (long) scaledPanelX) / panelMinBW);
        return xs;
    }

    /**
     * Rescale to actual screen coordinates - Create a copy
     * of array, and scale the copy's elements as Y coordinates.
     *
     * @param yorig Int array to be scaled; each member is a y-coordinate.
     * @return Scaled copy of yorig
     *
     * @see #scaleToActualY(int[])
     */
    public int[] scaleCopyToActualY(int[] yorig)
    {
        int[] ys = new int[yorig.length];
        for (int i = yorig.length - 1; i >= 0; --i)
            ys[i] = (int) ((yorig[i] * (long) scaledPanelY) / panelMinBH);
        return ys;
    }

    /**
     * Copy and rotate this array of y-coordinates, optionally also rescaling.
     * Rotates internal to actual (clockwise):  P'=(width-y, x)
     * @param yorig Array to copy and rotate, not null
     * @param width Width to rotate against
     * @param rescale Should we also rescale, same formula as {@link #scaleCopyToActualX(int[])}?
     * @return Rotated copy of <tt>yorig</tt> for use as x-coordinates
     * @since 1.1.08
     */
    public int[] rotateScaleCopyYToActualX(final int[] yorig, final int width, final boolean rescale)
    {
        int[] xr = new int[yorig.length];
        for (int i = yorig.length - 1; i >= 0; --i)
            xr[i] = width - yorig[i];
        if (rescale)
            for (int i = yorig.length - 1; i >= 0; --i)
                xr[i] = (int) ((xr[i] * (long) scaledPanelX) / panelMinBW);
        return xr;
    }

    /**
     * Redraw the board using double buffering. Don't call this directly, use
     * {@link Component#repaint()} instead.
     */
    public void paint(Graphics g)
    {
        try {
        if (buffer == null)
        {
            buffer = this.createImage(scaledPanelX, scaledPanelY);
        }
        drawBoard(buffer.getGraphics());
        if (hoverTip.isVisible())
            hoverTip.paint(buffer.getGraphics());
        buffer.flush();
        g.drawImage(buffer, 0, 0, this);

        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * Overriden so the peer isn't painted, which clears background. Don't call
     * this directly, use {@link Component#repaint()} instead.
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     * draw a board tile
     * @param g       graphics
     * @param hexNum  hex number (0-36)
     */
    private final void drawHex(Graphics g, int hexNum)
    {
        drawHex(g, hexX[hexNum], hexY[hexNum], board.getHexLayout()[hexNum], hexNum);
    }

    /**
     * draw a given type of board tile at a given location.
     * Use <tt>hexType</tt> to determine the hex graphic and possibly a port overlay graphic.
     * When <tt>hexNum</tt> is -1, nothing else is drawn.
     * Otherwise, also draw the dice number for this hex (if any).
     *
     * @param g      graphics
     * @param x      board-graphics x-coordinate to draw at
     * @param y      board-graphics y-coordinate to draw at
     * @param hexType hex number, as in {@link SOCBoard#getHexLayout()}
     * @param hexNum  hex number (0-36), or -1 if this isn't a valid hex number
     * @since 1.1.08
     */
    private final void drawHex(Graphics g, int x, int y, int hexType, int hexNum)
    {
        int tmp;

        if (isScaledOrRotated)
        {
            if (isRotated)
            {
                // (cw):  P'=(panelMinBH-y, x)
                int y1 = x;
                x = panelMinBH - y - HEXHEIGHT;  // move 1 hex over, since corner of image has rotated
                y = y1;
            }
            if (isScaled)
            {
                x = scaleToActualX(x);
                y = scaleToActualY(y);
            }
        }

        /**
         * If previous scaling has failed, fallback image will be smaller
         * compared to rest of the board graphics.  This is rare but
         * must be visually handled.  Center the smaller graphic in
         * the larger hex space, so it won't overlap other hexes.
         */
        boolean recenterPrevMiss = false;
        int xm=0, ym=0;  // offset for re-centering miss

        /**
         * If board is scaled, could be waiting for an image to resize.
         * If it still hasn't appeared after 7 seconds, give up and
         * create a new one.  (This can happen due to AWT bugs.)
         * drawBoard will then repaint with the new image.
         * If the new image also fails, a "fallback" will occur; a reference
         * to the static original-resolution image will be used.
         */
        boolean missedDraw = false;
        Image[] hexis = (isRotated ? rotatHexes : hexes);  // Fall back to original or rotated?

        tmp = hexType & 15; // get only the last 4 bits;

        if (isScaled && (scaledHexes[tmp] == hexis[tmp]))
        {
            recenterPrevMiss = true;
            int w = hexis[tmp].getWidth(null);
            int h = hexis[tmp].getHeight(null);
            xm = (scaleToActualX(w) - w) / 2;
            ym = (scaleToActualY(h) - h) / 2;
            x += xm;
            y += ym;
        }

        /**
         * Draw the hex graphic
         */
        if (! g.drawImage(scaledHexes[tmp], x, y, this))
        {
            // for now, draw the placeholder; try to rescale and redraw soon if we can

            g.translate(x, y);
            g.setColor(hexColor(hexType));
            g.fillPolygon(scaledHexCornersX, scaledHexCornersY, 6);
            g.setColor(Color.BLACK);
            g.drawPolyline(scaledHexCornersX, scaledHexCornersY, 6);
            g.translate(-x, -y);

            missedDraw = true;
            if (isScaled && (7000 < (drawnEmptyAt - scaledAt)))
            {
                // rescale the image or give up
                if (scaledHexFail[tmp])
                {
                    scaledHexes[tmp] = hexis[tmp];  // fallback
                }
                else
                {
                    scaledHexFail[tmp] = true;
                    int w = scaleToActualX(hexis[0].getWidth(null));
                    int h = scaleToActualY(hexis[0].getHeight(null));
                    scaledHexes[tmp] = hexis[tmp].getScaledInstance(w, h, Image.SCALE_SMOOTH);
                }
            }
        }
        if (recenterPrevMiss)
        {
            // Don't "center" further drawing
            x -= xm;
            y -= ym;
            recenterPrevMiss = false;
        }

        /**
         * Draw the port graphic
         */
        tmp = hexType >> 4; // get the facing of the port

        if (tmp > 0)
        {
            Image[] portis = (isRotated ? rotatPorts : ports);  // Fall back to original or rotated?
            if (isScaled && (scaledPorts[tmp] == portis[tmp]))
            {
                recenterPrevMiss = true;
                int w = portis[tmp].getWidth(null);
                int h = portis[tmp].getHeight(null);
                xm = (scaleToActualX(w) - w) / 2;
                ym = (scaleToActualY(h) - h) / 2;
                x += xm;
                y += ym;
            }
            if (! g.drawImage(scaledPorts[tmp], x, y, this))
            {
                g.drawImage(portis[tmp], x, y, null);  // show small port graphic, instead of a blank space
                missedDraw = true;
                if (isScaled && (7000 < (drawnEmptyAt - scaledAt)))
                {
                    if (scaledPortFail[tmp])
                    {
                        scaledPorts[tmp] = portis[tmp];  // fallback
                    }
                    else
                    {
                        scaledPortFail[tmp] = true;
                        int w = scaleToActualX(portis[1].getWidth(null));
                        int h = scaleToActualY(portis[1].getHeight(null));
                        scaledPorts[tmp] = portis[tmp].getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    }
                }
            }

            // Don't adj x,y to un-"center" further drawing:
            // If scaled, x,y will be recalculated anyway.
            // If not scaled, recenterPrevMiss is false.
        }

        if (hexNum == -1)
        {
            if (missedDraw)
            {
                // drawBoard will check this field after all hexes are drawn.
                scaledMissedImage = true;
            }
            return;  // <---- Early return: This hex isn't within hexLayout/numberLayout ----
        }

        /**
         * Draw the number
         */
        final int hnl = board.getNumberLayout()[hexNum];
        if (hnl > 0)
        {
            if (diceNumberCircleFont == null)
            {
                int fsize = DICE_NUMBER_FONTPOINTS;
                if (isScaled)
                    fsize = scaleToActualY(fsize);
                diceNumberCircleFont = new Font("Dialog", Font.BOLD, fsize);
            }
            if ((diceNumberCircleFM == null) && (diceNumberCircleFont != null))
            {
                diceNumberCircleFM = getFontMetrics(diceNumberCircleFont);
            }

            if ((diceNumberCircleFM != null) && (diceNumberCircleFont != null))
            {
                final int dx, dy;  // Offset of number graphic from upper-left corner of hex
                if (isRotated)
                {
                    dx = 22;  dy = 17;
                } else {
                    dx = 17;  dy = 22;
                }
    
                if (! isScaled)
                {
                    x += dx;
                    y += dy;
                }
                else
                {
                    x += scaleToActualX(dx);
                    y += scaleToActualY(dy);
                }
    
                // Draw the circle and dice number:
                int dia = DICE_NUMBER_CIRCLE_DIAMETER;
                if (isScaled)
                    dia = scaleToActualX(dia);
                ++dia;
    
                // Set background color:
                {
                    int colorIdx;
                    if (hnl < 7)
                        colorIdx = hnl - 2;
                    else
                        colorIdx = 12 - hnl;
                    g.setColor(DICE_NUMBER_CIRCLE_COLORS[colorIdx]);
                }
                g.fillOval(x, y, dia, dia);
                g.setColor(Color.BLACK);
                g.drawOval(x, y, dia, dia);
    
                final String numstr = Integer.toString(hnl);
                x += (dia - diceNumberCircleFM.stringWidth(numstr)) / 2;
                y += (dia + diceNumberCircleFM.getAscent() - diceNumberCircleFM.getDescent()) / 2;
                g.setFont(diceNumberCircleFont);
                g.drawString(numstr, x, y);

            }  // if (diceNumber fonts OK)
        }  // if (hnl > 0)

        if (missedDraw)
        {
            // drawBoard will check this field after all hexes are drawn.
            scaledMissedImage = true;
        }
    }

    /**
     * draw the robber
     * 
     * @param g       Graphics context
     * @param hexID   Board hex encoded position
     * @param fullNotGhost  Draw normally, not "ghost" of previous position
     *                (as during PLACE_ROBBER movement)
     */
    private final void drawRobber(Graphics g, int hexID, boolean fullNotGhost)
    {
        int hexNum = hexIDtoNum[hexID];
        int hx = hexX[hexNum] + 27;
        int hy = hexY[hexNum] + 32;
        if (isRotated)
        {
            // (cw):  P'=(panelMinBH-y, x)
            int hy1 = hx;
            hx = panelMinBH - hy;
            hy = hy1;
        }
        if (isScaled)
        {
            hx = scaleToActualX(hx);
            hy = scaleToActualY(hy);
        }

        Color rFill, rOutline;
        if (fullNotGhost)
        {
            rFill = Color.lightGray;
            rOutline = Color.black;
        } else {
            // Determine "ghost" color, we're moving the robber
            int hexType = board.getHexLayout()[hexNum];
            if (hexType >= robberGhostFill.length)
            {
                // should not happen
                rFill = Color.lightGray;
                rOutline = Color.black;
            } else if (robberGhostFill[hexType] != null)
            {
                // was cached from previous calculation
                rFill = robberGhostFill[hexType];
                rOutline = robberGhostOutline[hexType];
            } else {
                // find basic color, "ghost" it
                rOutline = hexColor(hexType);
                if (rOutline == ColorSquare.WATER)
                {
                    // Should not happen
                    rOutline = Color.lightGray;
                }

                // If hex is light, robber fill color should be dark. (average with gray)
                // If hex is dark or midtone, it should be light. (average with white)
                rFill = SOCPlayerInterface.makeGhostColor(rOutline);
                rOutline = rOutline.darker();  // Always darken the outline

                // Remember for next time
                robberGhostFill[hexType] = rFill;
                robberGhostOutline[hexType] = rOutline;
                
            }  // cached ghost color?
        }  // normal or ghost?

        g.translate(hx, hy);
        g.setColor(rFill);
        g.fillPolygon(scaledRobberX, scaledRobberY, 13);
        g.setColor(rOutline);
        g.drawPolygon(scaledRobberX, scaledRobberY, 14);
        g.translate(-hx, -hy);
    }

    /**
     * draw a road.
     * @param g  graphics
     * @param edgeNum  Edge number of this road; accepts -1 for edgeNum 0x00.
     * @param pn   Player number
     * @param isHilight  Is this the hilight for showing a potential placement?
     */
    private final void drawRoad(Graphics g, int edgeNum, int pn, boolean isHilight)
    {
        // Draw a road
        int hexNum, roadX[], roadY[];
        if (edgeNum == -1)
            edgeNum = 0x00;
        int dy = 0;  // y-offset, if edge's hex would draw it off the map

        if ((((edgeNum & 0x0F) + (edgeNum >> 4)) % 2) == 0)
        { // If first and second digit 
          // are even, then it is '|'.
            hexNum = hexIDtoNum[edgeNum + 0x11];
            roadX = scaledVertRoadX;
            roadY = scaledVertRoadY;
        }
        else if (((edgeNum >> 4) % 2) == 0)
        { // If first digit is even,
          // then it is '/'.
            if ((edgeNum >= 0x81) && (0 == ((edgeNum - 0x81) % 0x22)))
            {
                // hex is off the south edge of the board.
                // move 2 hexes north and offset y.
                hexNum = hexIDtoNum[edgeNum - 0x10 + 0x02];
                dy = 2 * deltaY;
            } else {
                hexNum = hexIDtoNum[edgeNum + 0x10];
            }
            roadX = scaledUpRoadX;
            roadY = scaledUpRoadY;
        }
        else
        { // Otherwise it is '\'.
            if ((edgeNum >= 0x18) && (0 == ((edgeNum - 0x18) % 0x22)))
            {
                // hex is off the north edge of the board.
                // move 2 hexes south and offset y.
                hexNum = hexIDtoNum[edgeNum + 0x20 - 0x01];
                dy = -2 * deltaY;
            } else {
                hexNum = hexIDtoNum[edgeNum + 0x01];
            }
            roadX = scaledDownRoadX;
            roadY = scaledDownRoadY;
        }
        int hx = hexX[hexNum];
        int hy = hexY[hexNum] + dy;
        if (isRotated)
        {
            // (cw):  P'=(panelMinBH-y, x)
            int hy1 = hx;
            hx = panelMinBH - hy - deltaX;  // -deltaX is because road poly coords are against hex width/height,
                                        // and the hex image gets similar translation in drawHex.
            hy = hy1;
        }
        if (isScaled)
        {
            hx = scaleToActualX(hx);
            hy = scaleToActualY(hy);
        }

        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, true));
        else
            g.setColor(playerInterface.getPlayerColor(pn));

        g.translate(hx, hy);
        g.fillPolygon(roadX, roadY, 5);
        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, false));
        else
            g.setColor(Color.black);
        g.drawPolygon(roadX, roadY, 5);
        g.translate(-hx, -hy);
    }

    /**
     * draw a settlement
     */
    private final void drawSettlement(Graphics g, int nodeNum, int pn, boolean isHilight)
    {
        drawSettlementOrCity(g, nodeNum, pn, isHilight, false);
    }

    /**
     * draw a city
     */
    private final void drawCity(Graphics g, int nodeNum, int pn, boolean isHilight)
    {
        drawSettlementOrCity(g, nodeNum, pn, isHilight, true);
    }

    /**
     * draw a settlement or city; they have the same logic for determining (x,y) from nodeNum.
     * @since 1.1.08
     */
    private final void drawSettlementOrCity
        (Graphics g, final int nodeNum, final int pn, final boolean isHilight, final boolean isCity)
    {
        int hexNum, hx, hy;

        if (((nodeNum >> 4) % 2) == 0)
        { // If first digit is even,
          // then it is a 'Y' node
          // in the northwest corner of a hex.
            if ((nodeNum >= 0x81) && (0 == ((nodeNum - 0x81) % 0x22)))
            {
                // this node's hex would be off the southern edge of the board.
                // shift 1 hex north, then add to y.
                hexNum = hexIDtoNum[nodeNum - 0x20 + 0x02 + 0x10]; 
                hx = hexX[hexNum];
                hy = hexY[hexNum] + 17 + (2 * deltaY);
            } else {
                hexNum = hexIDtoNum[nodeNum + 0x10]; 
                hx = hexX[hexNum];
                hy = hexY[hexNum] + 17;
            }
        }
        else
        { // otherwise it is an 'A' node
          // in the northern corner of a hex.
            if ((nodeNum >= 0x70) && (0 == ((nodeNum - 0x70) % 0x22)))
            {
                // this node's hex would be off the southern edge of the board.
                // shift 1 hex north, then add to y.
                hexNum = hexIDtoNum[nodeNum - 0x20 + 0x02 - 0x01];
                hx = hexX[hexNum] + halfdeltaX;
                hy = hexY[hexNum] + 2 + (2 * deltaY);
            }
            else if ((nodeNum & 0x0F) > 0)
            {
                hexNum = hexIDtoNum[nodeNum - 0x01];
                hx = hexX[hexNum] + halfdeltaX;
                hy = hexY[hexNum] + 2;
            } else {
                // this node's hex would be off the southwest edge of the board.
                // shift 1 hex to the east, then subtract from x.
                hexNum = hexIDtoNum[nodeNum + 0x22 - 0x01];
                hx = hexX[hexNum] - halfdeltaX;
                hy = hexY[hexNum] + 2;
            }
        }
        if (isRotated)
        {
            // (cw):  P'=(panelMinBH-y, x)
            int hy1 = hx;
            hx = panelMinBH - hy;
            hy = hy1;
        }
        if (isScaled)
        {
            hx = scaleToActualX(hx);
            hy = scaleToActualY(hy);
        }

        // System.out.println("NODEID = "+Integer.toHexString(nodeNum)+" | HEXNUM = "+hexNum);

        if (isCity)
        {
            g.translate(hx, hy);
            if (isHilight)
            {
                g.setColor(playerInterface.getPlayerColor(pn, true));
                g.drawPolygon(scaledCityX, scaledCityY, 8);
                // Draw again, slightly offset, for "ghost", since we can't fill and
                // cover up the underlying settlement.
                g.translate(1,1);
                g.drawPolygon(scaledCityX, scaledCityY, 8);
                g.translate(-(hx+1), -(hy+1));
                return;
            }

            g.setColor(playerInterface.getPlayerColor(pn));
            g.fillPolygon(scaledCityX, scaledCityY, 8);
            g.setColor(Color.black);
            g.drawPolygon(scaledCityX, scaledCityY, 8);
            g.translate(-hx, -hy);
        } else {
            // settlement
            if (isHilight)
                g.setColor(playerInterface.getPlayerColor(pn, true));
            else
                g.setColor(playerInterface.getPlayerColor(pn));
            g.translate(hx, hy);
            g.fillPolygon(scaledSettlementX, scaledSettlementY, 6);
            if (isHilight)
                g.setColor(playerInterface.getPlayerColor(pn, false));
            else
                g.setColor(Color.black);
            g.drawPolygon(scaledSettlementX, scaledSettlementY, 7);
            g.translate(-hx, -hy);
        }
    }

    /**
     * draw the arrow that shows whose turn it is.
     *
     * @param g Graphics
     * @param pnum Current player number.
     *             Player positions are clockwise from top-left:
     *           <BR>
     *             For the standard 4-player board:<BR>
     *             0 for top-left, 1 for top-right,
     *             2 for bottom-right, 3 for bottom-left
     *           <BR>
     *             For the 6-player board:<BR>
     *             0 for top-left, 1 for top-right, 2 for middle-right,
     *             3 for bottom-right, 4 for bottom-left, 5 for middle-left.
     * @param diceResult Roll result to show, if rolled.
     *                   To show, diceResult must be at least 2,
     *                   and gamestate not SOCGame.PLAY.
     */
    private final void drawArrow(Graphics g, int pnum, int diceResult)
    {
        int arrowX, arrowY, diceX, diceY;  // diceY always arrowY + 5
        boolean arrowLeft;

        // Player numbers are clockwise, starting at upper-left.
        // Since we have seats 0-3 in the corners already for 4-player,
        // just change pnum for 6-player.  Seats 0 and 1 need no change.
        // We'll use 4 for middle-right, and 5 for middle-left.

        if (is6player)
        {
            switch (pnum)
            {
            case 2:  // middle-right
                pnum = 4;  break;
            case 3:  // lower-right
                pnum = 2;  break;
            case 4:  // lower-left
                pnum = 3;  break;
            }
        }

        switch (pnum)
        {
        case 0:

            // top left
            arrowX = 3;  arrowY = 5;
            diceX = 13;
            arrowLeft = true;

            break;

        case 1:

            // top right
            arrowX = minSize.width - 40;  arrowY = 5;
            diceX = minSize.width - 40;
            arrowLeft = false;

            break;

        case 2:

            // bottom right
            arrowX = minSize.width - 40;  arrowY = minSize.height - 42;
            diceX = minSize.width - 40;
            arrowLeft = false;

            break;

        default:  // 3: (Default prevents compiler var-not-init errors)

            // bottom left
            arrowX = 3;  arrowY = minSize.height - 42;
            diceX = 13;
            arrowLeft = true;

            break;

        case 4:

            // middle right
            arrowX = minSize.width - 40;  arrowY = minSize.height / 2 - 12;
            diceX = minSize.width - 40;
            arrowLeft = false;
            break;

        case 5:

            // middle left
            arrowX = 3;  arrowY = minSize.height / 2 - 12;
            diceX = 13;
            arrowLeft = true;
            break;
        }

        diceY = arrowY + 5;

        /**
         * Draw Arrow
         */
        final int gameState = game.getGameState();
        if (isScaled)
        {
            arrowX = scaleToActualX(arrowX);
            arrowY = scaleToActualY(arrowY);
        }
        int[] scArrowX;
        if (arrowLeft)
            scArrowX = scaledArrowXL;
        else
            scArrowX = scaledArrowXR;
        g.translate(arrowX, arrowY);
        if (! (game.isSpecialBuilding() || (gameState == SOCGame.OVER)))
            g.setColor(ARROW_COLOR);
        else
            g.setColor(ARROW_COLOR_PLACING);        
        g.fillPolygon(scArrowX, scaledArrowY, scArrowX.length);
        g.setColor(Color.BLACK);
        g.drawPolygon(scArrowX, scaledArrowY, scArrowX.length);
        g.translate(-arrowX, -arrowY);

        /**
         * Draw Dice result number
         */
        if ((diceResult >= 2) && (gameState != SOCGame.PLAY) && (gameState != SOCGame.SPECIAL_BUILDING))
        {
            if (isScaled)
            {
                // Dice number is not scaled, but arrow is.
                // Move to keep centered in arrow.
                int adj = (scaleToActualX(DICE_SZ) - DICE_SZ) / 2;
                diceX = scaleToActualX(diceX) + adj;
                diceY = scaleToActualY(diceY) + adj;
            }
            g.drawImage(dice[diceResult], diceX, diceY, this);
        }
    }

    // TODO maybe move to socboard? otherwise refactor?
    /**
     * Hex numbers of start of each row of hexes in the board coordinates.
     * @since 1.1.08
     */
    private static final int[] ROW_START_HEXNUM = { 0, 4, 9, 15, 22, 28, 33 };

    /**
     * for the 6-player board, draw the ring of surrounding water/ports.
     * This is outside the coordinate system, and doesn't have hex numbers,
     * and so can't be drawn in the standard drawHex loop.
     * @since 1.1.08
     */
    private final void drawPortsRing(Graphics g)
    {
        int hnum, hx, hy, htype;

        /**
         * First, draw the ring of water hexes.
         * Then we'll overlay ports on them.
         */

        // To left of each of hex numbers: 0, 4, 9, 15, 22, 28, 33.
        for (int r = 0; r <= 6; ++r)
        {
            hnum = ROW_START_HEXNUM[r];

            // Water/port to left of hex row:
            hx = hexX[hnum] - deltaX;
            hy = hexY[hnum];
            drawHex(g, hx, hy, SOCBoard.WATER_HEX, -1);

            // Water/port to right of hex row:
            --hnum;  // is now rightmost hexnum of previous row
            if (hnum < 0)
                hnum = hexX.length - 1;  // wrap around
            hx = hexX[hnum];  // since the rightmost hexnum isn't within 6pl coord,
            hy = hexY[hnum];  // its (x,y) is right where we want to draw.
            drawHex(g, hx, hy, SOCBoard.WATER_HEX, -1);
        }

        hx = hexX[0] - halfdeltaX;
        hy = hexY[0] - deltaY;        // Above top row
        final int hy2 = hexY[33] + deltaY;  // Below bottom row

        for (int c = 0, nodeCoord = 0x07;
             c < 4;
             ++c, nodeCoord += 0x22, hx += deltaX)
        {
            htype = board.getPortTypeFromNodeCoord(nodeCoord);
            if (htype == -1)
                drawHex(g, hx, hy, SOCBoard.WATER_HEX, -1);

            // bottom-row coords swap the hex digits of top-row coords.
            htype = board.getPortTypeFromNodeCoord((nodeCoord >> 4) | ((nodeCoord & 0x0F) << 4));
            if (htype == -1)
                drawHex(g, hx, hy2, SOCBoard.WATER_HEX, -1);
        }

        /**
         * Draw each port
         */
        final int[] portsLayout = board.getPortsLayout();
        if (portsLayout == null)
            return;  // <--- Too early: board not created & sent from server ---

        final int[] portsFacing = board.getPortsFacing();
        final int[] portsEdges = board.getPortsEdges();
        for (int i = portsLayout.length-1; i>=0; --i)
        {
            // The (x,y) graphic location for this port isn't in hexX/hexY, because
            // the port is just beyond the coordinate system.  Get its facing land hex
            // and base (x,y) off that.
            final int landFacing = portsFacing[i];
            final int landHexCoord = board.getAdjacentHexToEdge(portsEdges[i], landFacing);
            hnum = board.getHexNumFromCoord(landHexCoord);
            // now move 1 hex "backwards" from hnum
            hx = hexX[hnum] - DELTAX_FACING[landFacing];
            hy = hexY[hnum] - DELTAY_FACING[landFacing];

            // portsLayout type will be like SHEEP_PORT;
            // must encode facing type and facing for drawHex.
            htype = portsLayout[i];
            if (htype == SOCBoard.MISC_PORT)
            {
                htype = SOCBoard.MISC_PORT_HEX + (landFacing - 1);
            } else {
                htype += (landFacing << 4) ;
            }
            drawHex(g, hx, hy, htype, -1);
        }
    }

    /**
     * draw the whole board, including pieces and tooltip if applicable.
     * The basic board without pieces is drawn just once, then buffered.
     * If the board layout changes (at start of game, for example),
     * call {@link #flushBoardLayoutAndRepaint()} to clear the buffered copy.
     *
     * @see #drawBoardEmpty(Graphics)
     */
    private void drawBoard(Graphics g)
    {
        if (scaledMissedImage || emptyBoardBuffer == null)
        {
            Image ebb = emptyBoardBuffer;  // Local copy, in case field becomes null in another thread
                                           // during drawBoardEmpty. (this has happened)
            if (ebb == null)
            {
                ebb = createImage(scaledPanelX, scaledPanelY);
                emptyBoardBuffer = ebb;
            }

            drawnEmptyAt = System.currentTimeMillis();
            scaledMissedImage = false;    // drawBoardEmpty, drawHex will set this flag if missed
            drawBoardEmpty(ebb.getGraphics());

            ebb.flush();
            if (scaledMissedImage && (7000 < (drawnEmptyAt - scaledAt)))
                scaledMissedImage = false;  // eventually give up scaling it
        }

        g.setPaintMode();
        g.drawImage(emptyBoardBuffer, 0, 0, this);

        int gameState = game.getGameState();

        if (board.getRobberHex() != -1)
        {
            drawRobber(g, board.getRobberHex(), (gameState != SOCGame.PLACING_ROBBER));
        }

        if (gameState != SOCGame.NEW)
        {
            drawArrow(g, game.getCurrentPlayerNumber(), game.getCurrentDice());
        }

        /**
         * draw the roads
         */
        Enumeration roads = board.getRoads().elements();

        while (roads.hasMoreElements())
        {
            SOCRoad r = (SOCRoad) roads.nextElement();
            drawRoad(g, r.getCoordinates(), r.getPlayer().getPlayerNumber(), false);
        }

        /**
         * draw the settlements
         */
        Enumeration settlements = board.getSettlements().elements();

        while (settlements.hasMoreElements())
        {
            SOCSettlement s = (SOCSettlement) settlements.nextElement();
            drawSettlement(g, s.getCoordinates(), s.getPlayer().getPlayerNumber(), false);
        }

        /**
         * draw the cities
         */
        Enumeration cities = board.getCities().elements();

        while (cities.hasMoreElements())
        {
            SOCCity c = (SOCCity) cities.nextElement();
            drawCity(g, c.getCoordinates(), c.getPlayer().getPlayerNumber(), false);
        }

        if (player != null)
        {
        /**
         * Draw the hilight when in interactive mode;
         * No hilight when null player (before game started).
         */
        switch (mode)
        {
        case PLACE_ROAD:
        case PLACE_INIT_ROAD:

            if (hilight != 0)
            {
                drawRoad(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case PLACE_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, otherPlayer.getPlayerNumber(), true);
            }

            break;

        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:

            if (hilight != 0)
            {
                drawRoad(g, hilight, otherPlayer.getPlayerNumber(), false);
            }

            break;

        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, otherPlayer.getPlayerNumber(), true);
            }

            break;

        case PLACE_ROBBER:

            if (hilight > 0)
            {
                drawRobber(g, hilight, true);
            }

            break;
        }  // switch
        }  // if (player != null)

        if (superText1 != null)
        {
            drawSuperText(g);
        }
        if (superTextTop != null)
        {
            drawSuperTextTop(g);
        }
    }

    /**
     * Draw the whole board (water, hexes, ports, numbers) but no placed pieces.
     * This is drawn once, then stored.
     * If the board layout changes (at start of game, for example),
     * call {@link #flushBoardLayoutAndRepaint()} to clear the buffered copy.
     * @param g Graphics, typically from {@link #emptyBoardBuffer}
     * @since 1.1.08
     */
    private void drawBoardEmpty(Graphics g)
    {
        g.setPaintMode();

        g.setColor(getBackground());
        g.fillRect(0, 0, scaledPanelX, scaledPanelY);

        // Draw hexes:
        // Normal board draws all 37 hexes.
        // The 6-player board skips the rightmost row (hexes 7D-DD-D7).
        // drawHex will set scaledMissedImage if missed.
        for (int i = 0; i < hexX.length; i++)
        {
            if ((inactiveHexNums == null) || ! inactiveHexNums[i])
                drawHex(g, i);
        }
        if (is6player)
            drawPortsRing(g);

        if (scaledMissedImage)
        {
            // With recent board resize, one or more rescaled images still hasn't
            // been completed after 7 seconds.  We've asked for a new scaled copy
            // of this image.  Repaint now, and repaint 3 seconds later.
            // (The delay gives time for the new scaling to complete.)
            scaledAt = System.currentTimeMillis();
            repaint();
            new DelayedRepaint(this).start();            
        }
    }

    /**
     * Draw {@link #superText1}, {@link #superText2}; if necessary, calculate {@link #superText1_w} and other fields.
     * @since 1.1.07
     */
    private void drawSuperText(Graphics g)
    {
        // Do we need to calculate the metrics?

        if ((superText1_w == 0) || ((superText2 != null) && (superText2_w == 0)))
        {
            final Font bpf = getFont();
            if (bpf == null)
            {
                repaint();  // We'll have to try again
                return;
            } else {
                final FontMetrics fm = getFontMetrics(bpf);
                if (fm == null)
                {
                    repaint();
                    return;  // We'll have to try again
                } else {
                    if (superText1_w == 0)
                    {
                        if (superText1 == null)
                            return;  // avoid NPE from multi-threading
                        superText1_w = fm.stringWidth(superText1);
                        superText_h = fm.getHeight();
                        superText_des = fm.getDescent();
                    }
                    if ((superText2 != null) && (superText2_w == 0))
                    {
                        superText2_w = fm.stringWidth(superText2);
                    }
                    // box size
                    if (superText2_w > superText1_w)
                        superTextBox_w = superText2_w;
                    else
                        superTextBox_w = superText1_w;
                    if (superText2 != null)
                        superTextBox_h = 2 * superText_h;
                    else
                        superTextBox_h = superText_h;

                    superTextBox_w += 2 * SUPERTEXT_INSET + 2 * SUPERTEXT_PADDING_HORIZ;
                    superTextBox_h += SUPERTEXT_INSET + 2 * fm.getDescent();

                    superTextBox_x = (scaledPanelX - superTextBox_w) / 2;
                    superTextBox_y = (scaledPanelY - superTextBox_h) / 2;
                }
            }
        }

        // adj from center
        g.setColor(Color.black);
        g.fillRoundRect(superTextBox_x, superTextBox_y, superTextBox_w, superTextBox_h, SUPERTEXT_INSET, SUPERTEXT_INSET);
        g.setColor(Color.white);
        g.fillRoundRect(superTextBox_x + SUPERTEXT_INSET, superTextBox_y + SUPERTEXT_INSET,
             superTextBox_w - 2 * SUPERTEXT_INSET, superTextBox_h - 2 * SUPERTEXT_INSET, SUPERTEXT_INSET, SUPERTEXT_INSET);
        g.setColor(Color.black);

        // draw text at center
        int tx = (scaledPanelX - superText1_w) / 2;
        int ty = superTextBox_y + SUPERTEXT_INSET + superText_h - superText_des;
        if (superText1 == null)
            return;  // avoid NPE from multi-threading
        g.drawString(superText1, tx, ty);
        if (superText2 != null)
        {
            tx -= (superText2_w - superText1_w) / 2;
            ty += superText_h;
            g.drawString(superText2, tx, ty);
        }
    }

    /**
     * Draw {@link #superTextTop}; if necessary, calculate {@link #superTextTop_w} and other fields.
     * @since 1.1.08
     */
    private void drawSuperTextTop(Graphics g)
    {
        // Force the font, so we know its metrics.
        // This avoids an OSX fm.stringWidth bug.
        final Font bpf = new Font("Dialog", Font.PLAIN, 10);

        // Do we need to calculate the metrics?

        if (superTextTop_w == 0)
        {
            final FontMetrics fm = g.getFontMetrics(bpf);
            if (fm == null)
            {
                repaint();
                return;  // We'll have to try again
            }
            if (superTextTop_w == 0)
            {
                if (superTextTop == null)
                    return;  // avoid NPE from multi-threading
                superTextTop_w = fm.stringWidth(superTextTop);
                superTextTop_h = fm.getHeight() - fm.getDescent();
            }

            // box size
            superTextTopBox_w = superTextTop_w;
            superTextTopBox_h = superTextTop_h;

            superTextTopBox_w += 2 * SUPERTEXT_INSET + 2 * SUPERTEXT_PADDING_HORIZ;
            superTextTopBox_h += SUPERTEXT_INSET + 2 * fm.getDescent();

            superTextTopBox_x = (scaledPanelX - superTextTopBox_w) / 2;
        }

        // adj from center
        g.setColor(Color.black);
        g.fillRoundRect(superTextTopBox_x, SUPERTEXT_INSET, superTextTopBox_w, superTextTopBox_h, SUPERTEXT_INSET, SUPERTEXT_INSET);
        g.setColor(Color.white);
        g.fillRoundRect(superTextTopBox_x + SUPERTEXT_INSET, 2 * SUPERTEXT_INSET,
             superTextTopBox_w - 2 * SUPERTEXT_INSET, superTextTopBox_h - 2 * SUPERTEXT_INSET, SUPERTEXT_INSET, SUPERTEXT_INSET);
        g.setColor(Color.black);
        // draw text at center
        int tx = (scaledPanelX - superTextTop_w) / 2;
        int ty = 2 * SUPERTEXT_INSET + superTextTop_h;
        g.setFont(bpf);
        if (superTextTop == null)
            return;  // avoid NPE from multi-threading
        g.drawString(superTextTop, tx, ty);

        /**
         * To debug OSX stringwidth... temp/in progress (20091129)
         *
        // green == box
        g.setColor(Color.green);
        g.drawLine(superTextTopBox_x, 0, superTextTopBox_x, 20);
        g.drawLine(superTextTopBox_x + superTextTopBox_w, 0, superTextTopBox_x + superTextTopBox_w, 20);
        // red == text
        g.setColor(Color.red);
        g.drawLine(tx, 0, tx, 20);
        g.drawLine(tx+superTextTop_w, 0, tx+superTextTop_w, 20);
        */
    }

    /**
     * Scale x-array from internal to actual screen-pixel coordinates.
     * If not isScaled, do nothing.
     *
     * @param xa Int array to be scaled; each member is an x-coordinate.
     *
     * @see #scaleCopyToActualX(int[])
     */
    public void scaleToActualX(int[] xa)
    {
        if (! isScaled)
            return;
        for (int i = xa.length - 1; i >= 0; --i)
            xa[i] = (int) ((xa[i] * (long) scaledPanelX) / panelMinBW);
    }

    /**
     * Scale y-array from internal to actual screen-pixel coordinates.
     * If not isScaled, do nothing.
     *
     * @param ya Int array to be scaled; each member is an y-coordinate.
     *
     * @see #scaleCopyToActualY(int[])
     */
    public void scaleToActualY(int[] ya)
    {
        if (! isScaled)
            return;
        for (int i = ya.length - 1; i >= 0; --i)
            ya[i] = (int) ((ya[i] * (long) scaledPanelY) / panelMinBH);
    }

    /**
     * Scale x-coordinate from internal to actual screen-pixel coordinates.
     * If not isScaled, return input.
     *
     * @param x x-coordinate to be scaled
     */
    public int scaleToActualX(int x)
    {
        if (! isScaled)
            return x;
        else
            return (int) ((x * (long) scaledPanelX) / panelMinBW);
    }

    /**
     * Scale y-coordinate from internal to actual screen-pixel coordinates.
     * If not isScaled, return input.
     *
     * @param y y-coordinate to be scaled
     */
    public int scaleToActualY(int y)
    {
        if (! isScaled)
            return y;
        else
            return (int) ((y * (long) scaledPanelY) / panelMinBH);
    }

    /**
     * Convert an x-coordinate from actual-scaled to internal-scaled coordinates.
     * If not isScaled, return input.
     *
     * @param x x-coordinate to be scaled
     */
    public int scaleFromActualX(int x)
    {
        if (! isScaled)
            return x;
        return (int) ((x * (long) panelMinBW) / scaledPanelX);
    }

    /**
     * Convert a y-coordinate from actual-scaled to internal-scaled coordinates.
     * If not isScaled, return input.
     *
     * @param y y-coordinate to be scaled
     */
    public int scaleFromActualY(int y)
    {
        if (! isScaled)
            return y;
        return (int) ((y * (long) panelMinBH) / scaledPanelY);
    }

    /**
     * Is the board currently scaled larger than
     * {@link #PANELX} x {@link #PANELY} pixels?
     * If so, use {@link #scaleToActualX(int)}, {@link #scaleFromActualY(int)},
     * etc to convert between internal and actual screen pixel coordinates.
     *
     * @return Is the board scaled larger than default size?
     * @see #isRotated()
     */
    public boolean isScaled()
    {
        return isScaled;
    }

    /**
     * Is the board currently rotated 90 degrees clockwise?
     * If so, the minimum size swaps {@link #PANELX} and {@link #PANELY}.
     *
     * @return Is the board rotated?
     * @see #isScaled()
     * @since 1.1.08
     */
    public boolean isRotated()
    {
        return isRotated;
    }

    /**
     * update the type of interaction mode.
     * Also calls {@link #updateHoverTipToMode()} and
     * (for 6-player board's Special Building Phase) updates top-center text.
     * For {@link soc.game.SOCGameOption#initAllOptions() Game Option "N7"},
     * updates the top-center countdown of rounds.
     */
    public void updateMode()
    {
        String topText = null;  // assume not Special Building Phase

        if (player != null)
        {
            final int cpn = game.getCurrentPlayerNumber();
            if (cpn == player.getPlayerNumber())
            {
                switch (game.getGameState())
                {
                case SOCGame.START1A:
                case SOCGame.START2A:
                    mode = PLACE_INIT_SETTLEMENT;

                    break;

                case SOCGame.START1B:
                case SOCGame.START2B:
                    mode = PLACE_INIT_ROAD;

                    break;

                case SOCGame.PLACING_ROAD:
                case SOCGame.PLACING_FREE_ROAD1:
                case SOCGame.PLACING_FREE_ROAD2:
                    mode = PLACE_ROAD;

                    break;

                case SOCGame.PLACING_SETTLEMENT:
                    mode = PLACE_SETTLEMENT;

                    break;

                case SOCGame.PLACING_CITY:
                    mode = PLACE_CITY;

                    break;

                case SOCGame.PLACING_ROBBER:
                    mode = PLACE_ROBBER;

                    break;
                    
                case SOCGame.NEW:
                case SOCGame.READY:
                    mode = GAME_FORMING;

                    break;

                case SOCGame.OVER:
                    mode = GAME_OVER;

                    break;

                case SOCGame.PLAY:
                    mode = TURN_STARTING;
                    if (game.isGameOptionSet("N7"))
                    {
                        // N7: Roll no 7s during first # rounds.
                        // Show if we can roll a 7 yet.  (1.1.09)

                        final int no7roundsleft = game.getGameOptionIntValue("N7") - game.getRoundCount();
                        if (no7roundsleft == 0)
                        {
                            topText = "Last round for \"No 7s\"";
                        } else if (no7roundsleft > 0)
                        {
                            if (playerInterface.clientIsCurrentPlayer()
                              && playerInterface.getClientHand().isClientAndCurrentlyCanRoll())
                                topText = (1 + no7roundsleft) + " rounds left for \"No 7s\"";
                        }
                    }
                    break;

                case SOCGame.SPECIAL_BUILDING:
                    mode = NONE;
                    topText = "Special Building: " + player.getName();
                    break;

                default:
                    mode = NONE;

                    break;
                }
            }
            else
            {
                // Not current player
                mode = NONE;

                if (game.isSpecialBuilding())
                {
                    topText = "Special Building: " + game.getPlayer(cpn).getName();
                }
                else if (game.isGameOptionSet("N7"))
                {
                    // N7: Roll no 7s during first # rounds.
                    // Show if we're about to be able to roll a 7.  (1.1.09)
                    final int no7roundsleft = game.getGameOptionIntValue("N7") - game.getRoundCount();
                    if (no7roundsleft == 0)
                        topText = "Last round for \"No 7s\"";
                }
            }
        }
        else
        {
            mode = NONE;
        }

        setSuperimposedTopText(topText);  // usually null
        updateHoverTipToMode();
    }
    
    protected void updateHoverTipToMode()
    {
        if ((mode == NONE) || (mode == TURN_STARTING) || (mode == GAME_OVER))            
            hoverTip.setOffsetX(0);
        else if (mode == PLACE_ROBBER)
            hoverTip.setOffsetX(HOVER_OFFSET_X_FOR_ROBBER);
        else if ((mode == PLACE_INIT_SETTLEMENT) || (mode == PLACE_INIT_ROAD))
            hoverTip.setOffsetX(HOVER_OFFSET_X_FOR_INIT_PLACE);
        else
            hoverTip.setHoverText(null);
    }

    /**
     * Set board mode to {@link #NONE}, no hilight, usually from a piece-placement mode.
     *
     * @param ptype Piece type to clear, like {@link SOCPlayingPiece#ROAD}, or -1 for robber.
     *              Used to avoid race condition during initial placement,
     *              where server has already replied with mode for another piece type,
     *              and board has already set mode to place that piece type.
     *              If ptype doesn't match the board's current mode/piece type,
     *              board's mode is not changed to NONE.
     */
    protected void clearModeAndHilight(final int ptype)
    {
        int expectedPtype;  // based on current mode

        switch (mode)
        {
        case PLACE_ROAD:
        case PLACE_INIT_ROAD:
        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:
            expectedPtype = SOCPlayingPiece.ROAD;
            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:
        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:
            expectedPtype = SOCPlayingPiece.SETTLEMENT;
            break;

        case PLACE_CITY:
        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:
            expectedPtype = SOCPlayingPiece.CITY;
            break;

        case PLACE_ROBBER:
            expectedPtype = -1;
            break;

        default:
            expectedPtype = ptype;  // Not currently placing
        }

        if (ptype == expectedPtype)
        {
            mode = NONE;
            hilight = 0;
        }
        updateHoverTipToMode();
    }

    /**
     * set the player that is using this board panel.
     */
    public void setPlayer()
    {
        player = game.getPlayer(playerInterface.getClient().getNickname());
        playerNumber = player.getPlayerNumber();
    }

    /**
     * set the other player
     *
     * @param op  the other player
     */
    public void setOtherPlayer(SOCPlayer op)
    {
        otherPlayer = op;
    }

    /*********************************
     * Handle Events
     *********************************/
    public void mouseEntered(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mousePressed(MouseEvent e)
    {
        ;  // JM: was mouseClicked (moved to avoid conflict with e.isPopupTrigger)
        mouseReleased(e);  // JM 2008-01-01 testing for MacOSX popup-trigger
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseReleased(MouseEvent e)
    {
        try {
        // Needed in Windows for popup-menu handling
        if (e.isPopupTrigger())
        {
            popupMenuSystime = e.getWhen();
            e.consume();
            doBoardMenuPopup(e.getX(), e.getY());
            return;
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseDragged(MouseEvent e)
    {
        ;
    }

    /**
     * Mouse has left the panel; hide tooltip and any hovering piece.
     *
     * @param e MouseEvent
     */
    public void mouseExited(MouseEvent e)
    {
        boolean wantsRepaint = false;
        if (hoverTip.isVisible())
        {
            hoverTip.hideHoverAndPieces();
            wantsRepaint = true;
        }
        if ((mode != NONE) && (mode != TURN_STARTING))
        {
            hilight = 0;
            wantsRepaint = true;
        }
        if (wantsRepaint)
            repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseMoved(MouseEvent e)
    {
        try {
        int x = e.getX();
        int y = e.getY();
        int xb, yb;
        if (isScaled)
        {
            xb = scaleFromActualX(x);
            yb = scaleFromActualY(y);
        }
        else
        {
            xb = x;
            yb = y;
        }
        if (isRotated)
        {
            // (ccw): P'=(y, panelMinBW-x)
            int xb1 = yb;
            yb = panelMinBW - xb - deltaY;  // -deltaY for similar reasons as -HEXHEIGHT in drawHex
            xb = xb1;
        }

        int edgeNum;
        int nodeNum;
        int hexNum;

        switch (mode)
        {
        case PLACE_INIT_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(xb, yb);

                // Figure out if this is a legal road
                // It must be attached to the last stlmt
                if ((player == null) ||
                     ! ((player.isPotentialRoad(edgeNum)) && ((edgeNum == initstlmt) || (edgeNum == (initstlmt - 0x11)) || (edgeNum == (initstlmt - 0x01)) || (edgeNum == (initstlmt - 0x10)))))
                {
                    edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case PLACE_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(xb, yb);

                if (edgeNum != 0)
                {
                    if ((player == null) || !player.isPotentialRoad(edgeNum))
                        edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(xb, yb);

                if ((player == null) || !player.isPotentialSettlement(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    if (mode == PLACE_INIT_SETTLEMENT)
                        hoverTip.handleHover(x,y);
                    repaint();
                }
                else if (mode == PLACE_INIT_SETTLEMENT)
                {
                    hoverTip.handleHover(x,y);  // Will call repaint() if needed
                }
            }

            break;

        case PLACE_CITY:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(xb, yb);

                if ((player == null) || !player.isPotentialCity(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case PLACE_ROBBER:

            /**** Code for finding a hex *********/
            hexNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                hexNum = findHex(xb, yb);

                if (! game.canMoveRobber(playerNumber, hexNum))
                {
                    hexNum = 0;
                }

                if (hilight != hexNum)
                {
                    hilight = hexNum;
                    hoverTip.handleHover(x,y);
                    repaint();
                }
                else
                {
                    hoverTip.positionToMouse(x,y); // calls repaint
                }
            }

            break;

        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(xb, yb);

                //if (!otherPlayer.isPotentialSettlement(nodeNum))
                //  nodeNum = 0;
                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(xb, yb);

                if (!otherPlayer.isPotentialRoad(edgeNum))
                {
                    edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(xb, yb);

                if (!otherPlayer.isPotentialCity(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case NONE:
        case TURN_STARTING:
        case GAME_OVER:
            // see hover
            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                hoverTip.handleHover(x,y);
            }
            
            break;
            
        case GAME_FORMING:
            // No hover for forming
            break;
        
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param evt DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent evt)
    {
        try {
        int x = evt.getX();
        int y = evt.getY();
        
        if (evt.isPopupTrigger())
        {
            popupMenuSystime = evt.getWhen();
            evt.consume();
            doBoardMenuPopup(x,y);
            return;  // <--- Pop up menu, nothing else to do ---
        }

        if (evt.getWhen() < (popupMenuSystime + POPUP_MENU_IGNORE_MS))
        {
            return;  // <--- Ignore click: too soon after popup click ---
        }

        if ((hilight != 0) && (player != null))
        {
            SOCPlayerClient client = playerInterface.getClient();

            switch (mode)
            {
            case NONE:
                break;

            case TURN_STARTING:
                break;

            case PLACE_INIT_ROAD:
            case PLACE_ROAD:

                if (hilight == -1)
                    hilight = 0;  // Road on edge 0x00
                if (player.isPotentialRoad(hilight))
                {
                    client.putPiece(game, new SOCRoad(player, hilight, board));

                    // Now that we've placed, clear the mode and the hilight.
                    clearModeAndHilight(SOCPlayingPiece.ROAD);
                }

                break;

            case PLACE_INIT_SETTLEMENT:
                initstlmt = hilight;
                // no break: fall through

            case PLACE_SETTLEMENT:

                if (player.isPotentialSettlement(hilight))
                {
                    client.putPiece(game, new SOCSettlement(player, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.SETTLEMENT);
                }

                break;

            case PLACE_CITY:

                if (player.isPotentialCity(hilight))
                {
                    client.putPiece(game, new SOCCity(player, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.CITY);
                }

                break;

            case PLACE_ROBBER:

                if (hilight != board.getRobberHex())
                {
                    client.moveRobber(game, player, hilight);
                    clearModeAndHilight(-1);
                }

                break;

            case CONSIDER_LM_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.SETTLEMENT);
                }

                break;

            case CONSIDER_LM_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.ROAD);
                }

                break;

            case CONSIDER_LM_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.CITY);
                }

                break;

            case CONSIDER_LT_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.SETTLEMENT);
                }

                break;

            case CONSIDER_LT_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.ROAD);
                }

                break;

            case CONSIDER_LT_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight, board));
                    clearModeAndHilight(SOCPlayingPiece.CITY);
                }

                break;
            }
        }
        else if ((player != null) && (game.getCurrentPlayerNumber() == player.getPlayerNumber()))
        {
            // No hilight. But, they clicked the board, expecting something.
            // It's possible the mode is incorrect.
            // Update and wait for the next click.
            updateMode();
            ptrOldX = 0;
            ptrOldY = 0;
            mouseMoved(evt);  // mouseMoved will establish hilight using click's x,y
        }
        
        evt.consume();
        
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }
    
    /**
     * Bring up the popup menu; called from mousePressed.
     *
     * @param x x-coordinate of click, actual screen pixels (not unscaled internal)
     * @param y y-coordinate of click, actual screen pixels (not unscaled internal)
     */
    protected void doBoardMenuPopup (int x, int y)
    {
        // Determine mode, to see if we're building or cancelling.
        switch (mode)
        {
        case PLACE_ROAD:
            popupMenu.showCancelBuild(SOCPlayingPiece.ROAD, x, y, hilight);
            break;

        case PLACE_SETTLEMENT:
            popupMenu.showCancelBuild(SOCPlayingPiece.SETTLEMENT, x, y, hilight);
            break;

        case PLACE_CITY:
            popupMenu.showCancelBuild(SOCPlayingPiece.CITY, x, y, hilight);
            break;
            
        case PLACE_INIT_ROAD:
            popupMenu.showBuild(x, y, hilight, 0, 0);
            break;
            
        case PLACE_INIT_SETTLEMENT:
            popupMenu.showBuild(x, y, 0, hilight, 0);
            break;
            
        default:  // NONE, GAME_FORMING, PLACE_ROBBER, etc
            popupMenu.showBuild(x, y, hoverTip.hoverRoadID, hoverTip.hoverSettlementID, hoverTip.hoverCityID);
        }
    }
    
    /** If the client has used the board popup menu to request building a piece,  
     *  this method is used in client network-receive message treatment.
     */
    public boolean popupExpectingBuildRequest()
    {
        if (buildReqTimerTask == null)
            return false;
        return ! buildReqTimerTask.wasItSentAlready();
    }

    public void popupSetBuildRequest(int coord, int ptype)
    {
        if (coord == -1)
            coord = 0;  // road on edge 0x00
        Timer piTimer = playerInterface.getEventTimer();
        synchronized (piTimer)
        {
            if (buildReqTimerTask != null)
            {
                buildReqTimerTask.doNotSend();
                buildReqTimerTask.cancel();  // cancel any previous
            }
            buildReqTimerTask = new BoardPanelSendBuildTask(coord, ptype);
            // Run once, at maximum permissable delay;
            // hopefully the network is responsive and
            // we've heard back by then.
            piTimer.schedule(buildReqTimerTask, 1000 * BUILD_REQUEST_MAX_DELAY_SEC );
        }
    }

    /**
     * player decided to not build something, so cancel the {@link TimerTask}
     * that's waiting to tell the server what they wanted to build.
     * @since 1.1.00
     */
    public void popupClearBuildRequest()
    {
        Timer piTimer = playerInterface.getEventTimer();
        synchronized (piTimer)
        {
            if (buildReqTimerTask == null)
                return;
            buildReqTimerTask.doNotSend();
            buildReqTimerTask.cancel();
            buildReqTimerTask = null;
        }
    }
    
    /** Have received gamestate placing message; send the building request in reply. */
    public void popupFireBuildingRequest()
    {
        Timer piTimer = playerInterface.getEventTimer();
        synchronized (piTimer)
        {
            if (buildReqTimerTask == null)
                return;
            buildReqTimerTask.sendOnceFromClientIfCurrentPlayer();
            buildReqTimerTask.cancel();
            buildReqTimerTask = null;
        }
        hoverTip.hideHoverAndPieces();  // Reset hover state
    }

    /**
     * Text to be displayed as 2 lines superimposed over the
     * center of the board graphic (during game setup).
     * Either text2, or both, can be null to display nothing.
     * Keep the text short, because boardPanel may not be very wide ({@link #PANELX} pixels).
     * Will trigger a repaint.
     * @param text1 Line 1 (or only line) of text, or null
     * @param text2 Line 2 of text, or null; must be null if text1 is null
     * @throws IllegalArgumentException if text1 null, text2 non-null
     * @since 1.1.07
     */
    public void setSuperimposedText(String text1, String text2)
        throws IllegalArgumentException
    {
        if ((superText1 == text1) && (superText2 == text2))
            return;
        if ((superText1 == null) && (superText2 != null))
            throw new IllegalArgumentException("text2 not null, text1 null");

        superText1 = text1;
        superText2 = text2;
        superText1_w = 0;
        superText2_w = 0;
        superTextBox_w = 0;
        repaint();
    }

    /**
     * Text to be displayed as 1 lines superimposed over the
     * top center of the board graphic (during game play).
     * text can be null to display nothing.
     * Keep the text short, because boardPanel may not be very wide ({@link #PANELX} pixels).
     * Will trigger a repaint.
     * @param text Line of text, or null
     * @since 1.1.08
     */
    public void setSuperimposedTopText(String text)
    {
        superTextTop = text;
        superTextTop_w = 0;
        superTextTopBox_w = 0;
        repaint();
    }

    /**
     * given a pixel on the board, find the edge that contains it
     *
     * @param x  x coordinate, in unscaled board, not actual pixels;
     *           use {@link #scaleFromActualX(int)} to convert
     * @param y  y coordinate, in unscaled board, not actual pixels
     * @return the coordinates of the edge, or 0 if none
     */
    private final int findEdge(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = (x / 18) + ((y / 10) * 15);
        int secX, secY;
        if (is6player)
        {
            secX = (x - HEXX_OFF_6PL) / 27;
            secY = (y - HEXY_OFF_6PL_FIND) / 15;
        } else {
            secX = x / 27;
            secY = y / 15;
        }
        int sector = secX + (secY * 15);

        // System.out.println("SECTOR = "+sector+" | EDGE = "+edgeMap[sector]);
        if ((sector >= 0) && (sector < edgeMap.length))
            return edgeMap[sector];
        else
            return 0;
    }

    /**
     * given a pixel on the board, find the node that contains it
     *
     * @param x  x coordinate, in unscaled board, not actual pixels;
     *           use {@link #scaleFromActualX(int)} to convert
     * @param y  y coordinate, in unscaled board, not actual pixels
     * @return the coordinates of the node, or 0 if none
     */
    private final int findNode(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = ((x + 9) / 18) + (((y + 5) / 10) * 15);
        int secX, secY;
        if (is6player)
        {
            secX = ((x + 13 - HEXX_OFF_6PL) / 27);
            secY = ((y + 7 - HEXY_OFF_6PL_FIND) / 15);
        } else {
            secX = ((x + 13) / 27);
            secY = ((y + 7) / 15);
        }
        int sector = secX + (secY * 15);

        // System.out.println("SECTOR = "+sector+" | NODE = "+nodeMap[sector]);
        if ((sector >= 0) && (sector < nodeMap.length))
            return nodeMap[sector];
        else
            return 0;
    }

    /**
     * given a pixel on the board, find the hex that contains it
     *
     * @param x  x coordinate, in unscaled board, not actual pixels;
     *           use {@link #scaleFromActualX(int)} to convert
     * @param y  y coordinate, in unscaled board, not actual pixels
     * @return the coordinates of the hex, or 0 if none
     */
    private final int findHex(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = (x / 18) + ((y / 10) * 15);
        int secX, secY;
        if (is6player)
        {
            secX = (x - HEXX_OFF_6PL) / 27;
            secY = (y - HEXY_OFF_6PL_FIND) / 15;
        } else {
            secX = x / 27;
            secY = y / 15;
        }
        int sector = secX + (secY * 15);

        // System.out.println("SECTOR = "+sector+" | HEX = "+hexMap[sector]);
        if ((sector >= 0) && (sector < hexMap.length))
            return hexMap[sector];
        else
            return 0;
    }

    /**
     * set the interaction mode
     *
     * @param m  mode
     * 
     * @see #updateMode()
     */
    public void setMode(int m)
    {
        mode = m;
        updateHoverTipToMode();
    }

    /**
     * get the interaction mode
     *
     * @return the mode
     */
    public int getMode()
    {
        return mode;
    }

    /**
     * load the images for the board
     * we need to know if this board is in an applet
     * or an application
     */
    private static synchronized void loadImages(Component c, boolean wantsRotated)
    {
        if ((hexes != null) && ((rotatHexes != null) || ! wantsRotated))
            return;

        Toolkit tk = c.getToolkit();
        Class clazz = c.getClass();

        if (hexes == null)
        {
            MediaTracker tracker = new MediaTracker(c);
        
            hexes = new Image[13];
            ports = new Image[7];
            dice = new Image[14];

            loadHexesPortsImages(hexes, ports, IMAGEDIR, tracker, tk, clazz);

            for (int i = 2; i < 13; i++)
            {
                dice[i] = tk.getImage(clazz.getResource(IMAGEDIR + "/dice" + i + ".gif"));
                tracker.addImage(dice[i], 0);
            }

            try
            {
                tracker.waitForID(0);
            }
            catch (InterruptedException e) {}

            if (tracker.isErrorID(0))
            {
                System.out.println("Error loading board images");
            }
        }

        if (wantsRotated && (rotatHexes == null))
        {
            MediaTracker tracker = new MediaTracker(c);

            rotatHexes = new Image[13];
            rotatPorts = new Image[7];
            loadHexesPortsImages(rotatHexes, rotatPorts, IMAGEDIR + "/rotat", tracker, tk, clazz);

            try
            {
                tracker.waitForID(0);
            }
            catch (InterruptedException e) {}

            if (tracker.isErrorID(0))
            {
                System.out.println("Error loading rotated board images");
            }
        }
    }

    /**
     * Load hex and port images from either normal, or rotated, resource location.
     * Remember that miscPort0.gif - miscPort5.gif are loaded into hexes, not ports.
     * @param newHexes Array to store hex images into; {@link #hexes} or {@link #rotatHexes}
     * @param newPorts Array to store port images into; {@link #ports} or {@link #rotatPorts}
     * @param imageDir Location for {@link Class#getResource(String)}
     * @param tracker Track image loading progress here
     * @param tk   Toolkit to load image from resource
     * @param clazz  Class for getResource
     * @since 1.1.08
     */
    private static final void loadHexesPortsImages
        (Image[] newHexes, Image[] newPorts, String imageDir,
         MediaTracker tracker, Toolkit tk, Class clazz)
    {
        newHexes[0] = tk.getImage(clazz.getResource(imageDir + "/desertHex.gif"));
        newHexes[1] = tk.getImage(clazz.getResource(imageDir + "/clayHex.gif"));
        newHexes[2] = tk.getImage(clazz.getResource(imageDir + "/oreHex.gif"));
        newHexes[3] = tk.getImage(clazz.getResource(imageDir + "/sheepHex.gif"));
        newHexes[4] = tk.getImage(clazz.getResource(imageDir + "/wheatHex.gif"));
        newHexes[5] = tk.getImage(clazz.getResource(imageDir + "/woodHex.gif"));
        newHexes[6] = tk.getImage(clazz.getResource(imageDir + "/waterHex.gif"));
        for (int i = 0; i < 7; i++)
        {
            tracker.addImage(newHexes[i], 0);
        }

        for (int i = 0; i < 6; i++)
        {
            newHexes[i + 7] = tk.getImage(clazz.getResource(imageDir + "/miscPort" + i + ".gif"));
            tracker.addImage(newHexes[i + 7], 0);
        }

        for (int i = 0; i < 6; i++)
        {
            newPorts[i + 1] = tk.getImage(clazz.getResource(imageDir + "/port" + i + ".gif"));
            tracker.addImage(newPorts[i + 1], 0);
        }
    }

    ///
    // ----- Utility methods -----
    ///

    /**
     * Hex color for a hex resource type
     * @param hexType  hexType value, as in {@link SOCBoard#DESERT_HEX}, {@link SOCBoard#WOOD_HEX},
     *                 {@link SOCBoard#WATER_HEX}.
     *                 Same value and meaning as those in {@link SOCBoard#getHexLayout()}.
     * @return The corresponding color from ColorSquare, or {@link ColorSquare#WATER} if hexType not recognized.
     * @since 1.1.07
     */
    public static final Color hexColor(int hexType)
    {
        Color hexColor;
        switch (hexType)
        {
        case SOCBoard.DESERT_HEX:
            hexColor = ColorSquare.DESERT;
            break;
        case SOCBoard.CLAY_HEX:
            hexColor = ColorSquare.CLAY;
            break;
        case SOCBoard.ORE_HEX:
            hexColor = ColorSquare.ORE;                
            break;
        case SOCBoard.SHEEP_HEX:
            hexColor = ColorSquare.SHEEP;
            break;
        case SOCBoard.WHEAT_HEX:
            hexColor = ColorSquare.WHEAT;
            break;
        case SOCBoard.WOOD_HEX:
            hexColor = ColorSquare.WOOD;
            break;
        default:  // WATER_HEX
            hexColor = ColorSquare.WATER;
        }
        return hexColor;
    }

    /**
     * With a recent board resize, one or more rescaled images still hasn't
     * been completed after 7 seconds.  We've asked for a new scaled copy
     * of this image.  Wait 3 seconds and repaint the board.
     * (The delay gives time for the new scaling to complete.)
     *
     * @see SOCBoardPanel#scaledMissedImage
     * @see SOCBoardPanel#drawHex(Graphics, int)
     * @author Jeremy D Monin <jeremy@nand.net>
     */
    protected static class DelayedRepaint extends Thread
    {
        /**
         * Are we already waiting in another thread?
         * Assumes since boolean is a simple var, will have atomic access. 
         */
        private static boolean alreadyActive = false;
        private SOCBoardPanel bp;

        public DelayedRepaint (SOCBoardPanel bp)
        {
            setDaemon(true);
            this.bp = bp;
        }

        public void run()
        {
            if (alreadyActive)
                return;

            alreadyActive = true;
            try
            {
                setName("delayedRepaint");
            }
            catch (Throwable th) {}
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {}
            finally
            {
                bp.repaint();
                alreadyActive = false;
            }
        }
    }  // static class DelayedRepaint



    protected class BoardToolTip
    {
        private SOCBoardPanel bpanel;
        
        /** Text to hover-display, or null if nothing to show */
        private String hoverText;

        /** Uses board mode constants: Will be {@link SOCBoardPanel#NONE NONE},
         *  {@link SOCBoardPanel#PLACE_ROAD PLACE_ROAD}, PLACE_SETTLEMENT,
         *  PLACE_ROBBER for hex, or PLACE_INIT_SETTLEMENT for port.
         */
        private int hoverMode;

        /** "ID" of coord as returned by {@link SOCBoardPanel#findNode(int, int) findNode}, findEdge, findHex */
        private int hoverID;

        /** Object last pointed at; null for hexes and ports */
        private SOCPlayingPiece hoverPiece;

        /** hover road ID, or 0. Readonly please from outside this inner class. Drawn in {@link #paint(Graphics)}.
         *  value is -1 for a road at edge 0x00.
         */
        int hoverRoadID;

        /** hover settlement or city node ID, or 0. Readonly please from outside this inner class. Drawn in {@link #paint(Graphics)}. */
        int hoverSettlementID, hoverCityID;

        /** is hover a port at coordinate hoverID? */
        boolean hoverIsPort;

        /** Mouse position */
        private int mouseX, mouseY;

        /** Our position (upper-left of tooltip box) */
        private int boxX, boxY;

        /** Requested X-offset from mouse pointer (used for robber placement) */
        private int offsetX;

        /** Our size.
         *  If boxw == 0, also indicates need fontmetrics - see setHoverText, paint.
         */
        private int boxW, boxH;
        
        private final int TEXT_INSET = 3;
        private final int PADDING_HORIZ = 2 * TEXT_INSET + 2;
        
        BoardToolTip(SOCBoardPanel ourBoardPanel)
        {
            bpanel = ourBoardPanel;
            hoverText = null;
            hoverMode = NONE;
            hoverID = 0;
            hoverPiece = null;
            hoverRoadID = 0;
            hoverSettlementID = 0;
            hoverCityID = 0;
            hoverIsPort = false;
            mouseX = 0;
            mouseY = 0;
            offsetX = 0;
            boxW = 0;
        }
        
        /** Currently displayed text.
         * 
         * @return Tooltip text, or null if nothing.
         */
        public String getHoverText()
        {
            return hoverText;
        }
        
        public boolean isVisible()
        {
            return ((hoverText != null) || (hoverRoadID != 0)
                    || (hoverSettlementID != 0) || (hoverCityID != 0));
        }

        /**
         * Show tooltip at appropriate location when mouse
         * is at (x,y) relative to the board.
         * @param x x-coordinate of mouse, actual screen pixels (not unscaled internal)
         * @param y y-coordinate of mouse, actual screen pixels (not unscaled internal)
         */
        public void positionToMouse(int x, int y)
        {
            mouseX = x;
            mouseY = y;

            boxX = mouseX + offsetX;
            boxY = mouseY;
            if (offsetX < 5)
                boxY += 12;

            if (panelMinBW < ( boxX + boxW ))
            {
                // Try to float it to left of mouse pointer
                boxX = mouseX - boxW - offsetX;
                if (boxX < 0)
                {
                    // Not enough room, just place flush against right-hand side
                    boxX = panelMinBW - boxW;
                }
            }
            
            bpanel.repaint();
            // JM TODO consider repaint(boundingbox).            
        }
        
        public void setOffsetX(int ofsX)
        {
            offsetX = ofsX;
        }

        /**
         * Set the hover text (tooltip) based on where the mouse is now,
         * and repaint the board.
         * @param t Hover text contents, or null to clear that text (but
         *          not hovering pieces) and repaint
         * @see #hideHoverAndPieces()
         */
        public void setHoverText(String t)
        {
            hoverText = t;
            if (t == null)
            {
                bpanel.repaint();
                return;
            }

            final Font bpf = bpanel.getFont();
            if (bpf == null)
            {
                boxW = 0;  // Paint method will look it up
            } else {
                final FontMetrics fm = getFontMetrics(bpf);
                if (fm == null)
                {
                    boxW = 0;
                } else {
                    boxW = fm.stringWidth(hoverText) + PADDING_HORIZ;
                    boxH = fm.getHeight();
                }
            }
            positionToMouse(mouseX, mouseY);  // Also calls repaint
        }
        
        /** Clear hover text, and cancel any hovering roads/settlements/cities */
        public void hideHoverAndPieces()
        {
            hoverRoadID = 0;
            hoverSettlementID = 0;
            hoverCityID = 0;
            hoverIsPort = false;
            setHoverText(null);
        }
        
        /** Draw; Graphics should be the boardpanel's gc, as seen in its paint method. */
        public void paint(Graphics g)
        {
            if (playerNumber != -1)
            {
                if (hoverRoadID != 0)
                {
                    drawRoad(g, hoverRoadID, playerNumber, true);
                }
                if (hoverSettlementID != 0)
                {
                    drawSettlement(g, hoverSettlementID, playerNumber, true);
                }
                if (hoverCityID != 0)
                {
                    drawCity(g, hoverCityID, playerNumber, true);
                }
            }
            String ht = hoverText;  // cache against last-minute change in another thread
            if (ht == null)
                return;

            if (boxW == 0)
            {
                // Deferred fontmetrics lookup from earlier setHoverText
                final Font bpf = bpanel.getFont();
                if (bpf == null)
                    return;
                final FontMetrics fm = getFontMetrics(bpf);
                if (fm == null)
                    return;
                boxW = fm.stringWidth(hoverText) + PADDING_HORIZ;
                boxH = fm.getHeight();
            }
            g.setColor(Color.WHITE);
            g.fillRect(boxX, boxY, boxW - 1, boxH - 1);
            g.setColor(Color.BLACK);
            g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
            g.drawString(ht, boxX + TEXT_INSET, boxY + boxH - TEXT_INSET);
        }

        /**
         * Mouse is hovering during normal play; look for info for tooltip text.
         * Assumes x or y has changed since last call.
         * Does not affect the "hilight" variable used by SOCBoardPanel during
         * initial placement, and during placement from clicking "Buy" buttons.
         *<P>
         * If the board mode doesn't allow hovering pieces (ghost pieces), will clear
         * hoverRoadID, hoverSettlementID, and hoverCityID to 0.
         * Otherwise, these are set when the mouse is at a location where the
         * player can build or upgrade, and they have resources to build.
         *
         * @param x Cursor x, from upper-left of board: actual coordinates, not board-internal coordinates
         * @param y Cursor y, from upper-left of board: actual coordinates, not board-internal coordinates
         */
        private void handleHover(int x, int y)
        {
            mouseX = x;
            mouseY = y;
            int xb = x, yb = y;  // internal board coordinates
            if (isScaledOrRotated)
            {
                if (isScaled)
                {
                    xb = scaleFromActualX(xb);
                    yb = scaleFromActualY(yb);
                }
                if (isRotated)
                {
                    // (ccw): P'=(y, panelMinBW-x)
                    int xb1 = yb;
                    yb = panelMinBW - xb - deltaY;  // -deltaY for similar reasons as -HEXHEIGHT in drawHex
                    xb = xb1;
                }
            }

            // Variables set in previous call to handleHover:
            // hoverMode, hoverID, hoverText.
            // Check whether they have changed.
            // If not, just move the tooltip with positionToMouse.

            /** Coordinates on board (a node, edge, or hex) */
            int id;
            boolean modeAllowsHoverPieces = ((mode != PLACE_INIT_SETTLEMENT)
                && (mode != PLACE_INIT_ROAD) && (mode != PLACE_ROBBER)
                && (mode != TURN_STARTING) && (mode != GAME_OVER));

            boolean playerIsCurrent = (player != null) && playerInterface.clientIsCurrentPlayer();
            boolean hoverTextSet = false;  // True once text is determined

            if (! modeAllowsHoverPieces)
            {
                hoverRoadID = 0;
                hoverSettlementID = 0;
                hoverCityID = 0;
            }

            // Look first for settlements
            id = findNode(xb,yb);
            if (id > 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_SETTLEMENT) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }
                
                // Is anything there?
                SOCPlayingPiece p = board.settlementAtNode(id);
                if (p != null)
                {
                    hoverMode = PLACE_SETTLEMENT;
                    hoverPiece = p;
                    hoverID = id;

                    StringBuffer sb = new StringBuffer();
                    String portDesc = portDescAtNode(id);
                    if (portDesc != null)
                    {
                        sb.append(portDesc);  // "3:1 Port", "2:1 Wood port"
                        if (p.getType() == SOCPlayingPiece.CITY)
                            sb.append(" city: ");
                        else
                            sb.append(": ");  // port, not port city
                        hoverIsPort = true;
                    }
                    else
                    {
                        if (p.getType() == SOCPlayingPiece.CITY)
                            sb.append("City: ");
                        else
                            sb.append("Settlement: ");
                    }
                    String plName = p.getPlayer().getName();
                    if (plName == null)
                        plName = "unowned";
                    sb.append(plName);
                    setHoverText(sb.toString());
                    hoverTextSet = true;

                    // If we're at the player's settlement, ready to upgrade to city
                    if (modeAllowsHoverPieces && playerIsCurrent
                         && (p.getPlayer() == player)
                         && (p.getType() == SOCPlayingPiece.SETTLEMENT)
                         && (player.isPotentialCity(id))
                         && (player.getResources().contains(SOCGame.CITY_SET)))
                    {
                        hoverCityID = id;
                    } else {
                        hoverCityID = 0;
                    }
                    hoverSettlementID = 0;
                }
                else {
                    if (playerIsCurrent)
                    {
                        // Nothing currently here.
                        hoverCityID = 0;
                        if (modeAllowsHoverPieces
                            && player.isPotentialSettlement(id)
                            && player.getResources().contains(SOCGame.SETTLEMENT_SET))
                            hoverSettlementID = id;
                        else
                            hoverSettlementID = 0;
                    }
                    
                    // Port check.  At most one adjacent will be a port.
                    if ((hoverMode == PLACE_INIT_SETTLEMENT) && (hoverID == id))
                    {
                        // Already looking at a port at this coordinate.
                        positionToMouse(x,y);
                        hoverTextSet = true;
                    }
                    else
                    {
                        String portDesc = portDescAtNode(id);
                        if (portDesc != null)
                        {
                            setHoverText(portDesc);
                            hoverTextSet = true;
                            hoverMode = PLACE_INIT_SETTLEMENT;  // const used for hovering-at-port
                            hoverID = id;
                            hoverIsPort = true;
                        }
                    }
                }  // end if-node-has-settlement
            }
            else
            {
                hoverSettlementID = 0;
                hoverCityID = 0;                
            }

            // If not over a settlement, look for a road
            id = findEdge(xb,yb);
            if (id != 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_ROAD) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }

                // Is anything there?
                SOCPlayingPiece p = board.roadAtEdge(id);
                if (p != null)
                {
                    if (! hoverTextSet)
                    {
                        hoverMode = PLACE_ROAD;
                        hoverPiece = p;
                        hoverID = id;
                        String plName = p.getPlayer().getName();
                        if (plName == null)
                            plName = "unowned";
                        setHoverText("Road: " + plName);
                    }
                    hoverRoadID = 0;
                    
                    return;  // <--- Early return: Found road ---
                }
                else if (playerIsCurrent)
                {
                    // No piece there
                    if (modeAllowsHoverPieces
                        && player.isPotentialRoad(id)
                        && player.getResources().contains(SOCGame.ROAD_SET))
                        hoverRoadID = id;
                    else
                        hoverRoadID = 0;
                }
            }
            
            // By now we've set hoverRoadID, hoverCityID, hoverSettlementID, hoverIsPort.
            if (hoverTextSet)
            {
                return;  // <--- Early return: Text and hover-pieces set ---
            }

            // If no road, look for a hex
            //  - reminder: socboard.getHexTypeFromCoord, getNumberOnHexFromCoord, socgame.getPlayersOnHex
            id = findHex(xb,yb);
            if (id > 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_ROBBER) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }
                
                hoverMode = PLACE_ROBBER;  // const used for hovering-at-hex
                hoverPiece = null;
                hoverID = id;
                {
                    StringBuffer sb = new StringBuffer();
                    switch (board.getHexTypeFromCoord(id))
                    {
                    case SOCBoard.DESERT_HEX:
                        sb.append("Desert");  break;
                    case SOCBoard.CLAY_HEX:
                        sb.append("Clay");    break;
                    case SOCBoard.ORE_HEX:
                        sb.append("Ore");     break;
                    case SOCBoard.SHEEP_HEX:
                        sb.append("Sheep");   break;
                    case SOCBoard.WHEAT_HEX:
                        sb.append("Wheat");   break;
                    case SOCBoard.WOOD_HEX:                     
                        sb.append("Wood");    break;
                    case SOCBoard.WATER_HEX:
                        sb.append("Water");   break;
                    default:
                        {
                            final int htype = board.getHexTypeFromCoord(id);
                            String portDesc = null;
                            if ((htype >= SOCBoard.MISC_PORT_HEX) && (htype <= SOCBoard.WOOD_PORT_HEX))
                            {
                                portDesc = portDescForType(htype - (SOCBoard.MISC_PORT_HEX - SOCBoard.MISC_PORT));
                            }
                            if (portDesc != null)
                            {
                                sb.append(portDesc);
                            } else {
                                sb.append("Hex type ");
                                sb.append(htype);
                            }
                        }
                    }
                    if (board.getRobberHex() == id)
                    {
                        int num = board.getNumberOnHexFromCoord(id);
                        if (num > 0)
                        {
                            sb.append(": ");
                            sb.append(num);
                        }
                        sb.append(" (ROBBER)");
                    }
                    setHoverText(sb.toString());                     
                }
                
                return;  // <--- Early return: Found hex ---
            }

            if (hoverRoadID != 0)
            {
                setHoverText(null); // hoverMode = PLACE_ROAD;
                bpanel.repaint();
                return;
            }

            // If no hex, nothing.
            hoverMode = NONE;
            setHoverText(null);
        }

        /**
         * Check at this node coordinate for a port, and return its descriptive text.
         * Does not check for players' settlements or cities, only for the port.
         *
         * @param id Node coordinate ID for potential port
         *
         * @return Port text description, or null if no port at that node id.
         *    Text format is "3:1 Port" or "2:1 Wood port".
         */
        public String portDescAtNode(int id)
        {
            return portDescForType(board.getPortTypeFromNodeCoord(id));
        }

        /**
         * Descriptive text for a given port type.
         * @param portType Port type, as from {@link SOCBoard#getPortTypeFromNodeCoord(int)}.
         *           Should be in range {@link SOCBoard#MISC_PORT} to {@link SOCBoard#WOOD_PORT}.
         * @return Port text description, or null if no port for that value of <tt>portType</tt>
         *    Text format is "3:1 Port" or "2:1 Wood port".
         * @since 1.1.08
         */
        public String portDescForType(final int portType)
        {
            if (portType == -1)
                return null;  // <--- No port found ---

            String portDesc;
            switch (portType)
            {
            case SOCBoard.MISC_PORT:
                portDesc = "3:1 Port";
                break;

            case SOCBoard.CLAY_PORT:
                portDesc = "2:1 Clay port";
                break;

            case SOCBoard.ORE_PORT:
                portDesc = "2:1 Ore port";
                break;

            case SOCBoard.SHEEP_PORT:
                portDesc = "2:1 Sheep port";
                break;

            case SOCBoard.WHEAT_PORT:
                portDesc = "2:1 Wheat port";
                break;

            case SOCBoard.WOOD_PORT:
                portDesc = "2:1 Wood port";
                break;                            

            default:
                // Just in case
                portDesc = "port type " + portType;                        
            }

            return portDesc;
        }
        
    }  // inner class BoardToolTip



    /**
     * This class creates a popup menu on the board,
     * to trade or build or cancel building.
     */
    private class BoardPopupMenu extends PopupMenu
        implements java.awt.event.ActionListener
    {
      /** our parent boardpanel */
      SOCBoardPanel bp;

      MenuItem buildRoadItem, buildSettleItem, upgradeCityItem;
      MenuItem cancelBuildItem;

      /** determined at menu-show time, only over a useable port. Added then, and removed at next menu-show */
      SOCHandPanel.ResourceTradePopupMenu portTradeSubmenu;

      /** determined at menu-show time */
      private int menuPlayerID;

      /** determined at menu-show time */
      private boolean menuPlayerIsCurrent;

      /** determined at menu-show time */
      private boolean wantsCancel;

      private int cancelBuildType;

      /** hover road ID, or 0, at menu-show time */
      private int hoverRoadID;

      /** hover settlement or city node ID, or 0, at menu-show time */
      private int hoverSettlementID, hoverCityID;

      /** Will this be for initial placement (send putpiece right away),
       *  or for placement during game (send build, receive gamestate, send putpiece)?
       */
      protected boolean isInitialPlacement;

      /** create a new BoardPopupMenu on this board */
      public BoardPopupMenu(SOCBoardPanel bpanel)
      {
        super ("JSettlers");
        bp = bpanel;

        buildRoadItem = new MenuItem("Build Road");         
        buildSettleItem = new MenuItem("Build Settlement");
        upgradeCityItem = new MenuItem("Upgrade to City");
        cancelBuildItem = new MenuItem("Cancel build");
        portTradeSubmenu = null;

        add(buildRoadItem);
        add(buildSettleItem);
        add(upgradeCityItem);
        addSeparator();
        add(cancelBuildItem);

        buildRoadItem.addActionListener(this);
        buildSettleItem.addActionListener(this);
        upgradeCityItem.addActionListener(this);
        cancelBuildItem.addActionListener(this);
      }

      /** Custom 'cancel' show method for when placing a road/settlement/city,
       *  giving the build/cancel options for that type of piece.
       * 
       * @param buildType piece type (SOCPlayingPiece.ROAD, CITY, SETTLEMENT)
       * @param x   Mouse x-position
       * @param y   Mouse y-position
       * @param hilightAt Current hover/hilight coordinates of piece being cancelled/placed
       */
      public void showCancelBuild(int buildType, int x, int y, int hilightAt)
      {
          menuPlayerIsCurrent = (player != null) && playerInterface.clientIsCurrentPlayer();
          wantsCancel = true;
          cancelBuildType = buildType;
          hoverRoadID = 0;
          hoverSettlementID = 0;
          hoverCityID = 0;

          buildRoadItem.setEnabled(false);
          buildSettleItem.setEnabled(false);
          upgradeCityItem.setEnabled(false);
          cancelBuildItem.setEnabled(menuPlayerIsCurrent);

          // Check for initial placement (for different cancel message)
          switch (game.getGameState())
          {
          case SOCGame.START1A:
          case SOCGame.START2A:
          case SOCGame.START1B:
          case SOCGame.START2B:
              isInitialPlacement = true;
              break;
          
          default:
              isInitialPlacement = false;
          }

          switch (buildType)
          {
          case SOCPlayingPiece.ROAD:
              cancelBuildItem.setLabel("Cancel road");
              buildRoadItem.setEnabled(menuPlayerIsCurrent);
              hoverRoadID = hilightAt; 
              break;

          case SOCPlayingPiece.SETTLEMENT:
              cancelBuildItem.setLabel("Cancel settlement");
              buildSettleItem.setEnabled(menuPlayerIsCurrent);
              hoverSettlementID = hilightAt; 
              break;

          case SOCPlayingPiece.CITY:
              cancelBuildItem.setLabel("Cancel city upgrade");
              upgradeCityItem.setEnabled(menuPlayerIsCurrent);
              hoverCityID = hilightAt;
              break;

          default:
              throw new IllegalArgumentException ("bad buildtype: " + buildType);
          }

          super.show(bp, x, y);
      }
      
      /**
       * Custom show method that finds current game status and player status.
       * Also checks for hovering-over-port for port-trade submenu.
       *
       * @param x   Mouse x-position
       * @param y   Mouse y-position
       * @param hR  Hover road ID, or 0
       * @param hS  Hover settle ID, or 0
       * @param hC  Hover city ID, or 0
       */
      public void showBuild(int x, int y, int hR, int hS, int hC)
      {
          wantsCancel = false;
          isInitialPlacement = false;
          cancelBuildItem.setEnabled(false);
          cancelBuildItem.setLabel("Cancel build");
          if (portTradeSubmenu != null)
          {
              // Cleanup from last time
              remove(portTradeSubmenu);
              portTradeSubmenu.destroy();
              portTradeSubmenu = null;
          }
         
          menuPlayerIsCurrent = (player != null) && playerInterface.clientIsCurrentPlayer();
          if (menuPlayerIsCurrent)
          {
              int gs = game.getGameState();
              switch (gs)
              {
              case SOCGame.START1A:
              case SOCGame.START2A:
                  isInitialPlacement = true;  // Settlement
                  buildRoadItem.setEnabled(false);
                  buildSettleItem.setEnabled(hS != 0);
                  upgradeCityItem.setEnabled(false);
                  break;

              case SOCGame.START1B:
              case SOCGame.START2B:
                  isInitialPlacement = true;  // Road
                  buildRoadItem.setEnabled(hR != 0);
                  buildSettleItem.setEnabled(false);
                  upgradeCityItem.setEnabled(false);
                  cancelBuildItem.setLabel("Cancel settlement");  // Initial settlement
                  cancelBuildItem.setEnabled(true);
                  cancelBuildType = SOCPlayingPiece.SETTLEMENT;
                  break;
              
              default:
                  if (gs < SOCGame.PLAY1)
                      menuPlayerIsCurrent = false;  // Not in a state to place items
              }
          }
          
          if (! menuPlayerIsCurrent)
          {
              buildRoadItem.setEnabled(false);
              buildSettleItem.setEnabled(false);
              upgradeCityItem.setEnabled(false);
              hoverRoadID = 0;
              hoverSettlementID = 0;
              hoverCityID = 0;
          }
          else
          {
              int cpn = game.getCurrentPlayerNumber();

              if (! isInitialPlacement)
              {
                  buildRoadItem.setEnabled(game.couldBuildRoad(cpn) && player.isPotentialRoad(hR));
                  buildSettleItem.setEnabled(game.couldBuildSettlement(cpn) && player.isPotentialSettlement(hS));
                  upgradeCityItem.setEnabled(game.couldBuildCity(cpn) && player.isPotentialCity(hC));
              }
              hoverRoadID = hR;
              hoverSettlementID = hS;
              hoverCityID = hC;
              
              // Is it a port?
              int portType = -1;
              int portId = 0;
              if (hS != 0)
                  portId = hS;
              else if (hC != 0)
                  portId = hC;
              else if (bp.hoverTip.hoverIsPort)
                  portId = bp.hoverTip.hoverID;

              if (portId != 0)
                  portType = board.getPortTypeFromNodeCoord(portId);

              // Menu differs based on port
              if (portType != -1)
              {
                  if (portType == SOCBoard.MISC_PORT)
                      portTradeSubmenu = new ResourceTradeAllMenu
                          (bp, playerInterface.getPlayerHandPanel(cpn));
                  else
                      portTradeSubmenu = new SOCHandPanel.ResourceTradeTypeMenu
                          (playerInterface.getPlayerHandPanel(cpn), portType, false);                  
                  add(portTradeSubmenu);
                  portTradeSubmenu.setEnabledIfCanTrade(true);
              }
          }

          super.show(bp, x, y);
      }

      /** Handling the menu items **/
      public void actionPerformed(ActionEvent e)
      {
          if (! playerInterface.clientIsCurrentPlayer())
              return;
          if (! menuPlayerIsCurrent)
              return;
          Object target = e.getSource();
          if (target == buildRoadItem)
              tryBuild(SOCPlayingPiece.ROAD);
          else if (target == buildSettleItem)
              tryBuild(SOCPlayingPiece.SETTLEMENT);
          else if (target == upgradeCityItem)
              tryBuild(SOCPlayingPiece.CITY);
          else if (target == cancelBuildItem)
              tryCancel();
      } 

      /**
       * Send message to server to request placing this piece, if allowable.
       * If not initial placement, set up a reaction to send the 2nd message (putpiece).
       * when server says it's OK to build.
       * Assumes player is current, and player is non-null, when called.
       *
       * @param ptype Piece type, like {@link SOCPlayingPiece#ROAD}
       */
      void tryBuild(int ptype)
      {
          int cpn = playerInterface.getClientPlayerNumber();
          int buildLoc;      // location
          boolean canBuild;  // resources, rules
          String btarget;    // button name on buildpanel
          
          // If we're in initial placement, or cancel/build during game, send putpiece right now.
          // Otherwise, multi-phase send.
          
          // Note that if we're in gameplay have clicked the "buy road" button
          // and trying to place it, game.couldBuildRoad will be false because
          // we've already spent the resources.  So, wantsCancel won't check it.
          
          switch (ptype)
          {
          case SOCPlayingPiece.ROAD:
              buildLoc = hoverRoadID;
              canBuild = player.isPotentialRoad(buildLoc);
              if (! (isInitialPlacement || wantsCancel))
                  canBuild = canBuild && game.couldBuildRoad(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
                  playerInterface.getClient().putPiece(game, new SOCRoad(player, buildLoc, board));
              btarget = SOCBuildingPanel.ROAD;
              break;

          case SOCPlayingPiece.SETTLEMENT:
              buildLoc = hoverSettlementID;
              canBuild = player.isPotentialSettlement(buildLoc);
              if (! (isInitialPlacement || wantsCancel))
                  canBuild = canBuild && game.couldBuildSettlement(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
              {
                  playerInterface.getClient().putPiece(game, new SOCSettlement(player, buildLoc, board));
                  if (isInitialPlacement)
                      initstlmt = buildLoc;  // track for initial road mouseover hilight
              }
              btarget = SOCBuildingPanel.STLMT;
              break;
          
          case SOCPlayingPiece.CITY:
              buildLoc = hoverCityID;
              canBuild = game.couldBuildCity(cpn) && player.isPotentialCity(buildLoc);
              if (! (isInitialPlacement || wantsCancel))             
                  canBuild = canBuild && game.couldBuildCity(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
                  playerInterface.getClient().putPiece(game, new SOCCity(player, buildLoc, board));
              btarget = SOCBuildingPanel.CITY;
              break;

          default:
              throw new IllegalArgumentException ("Bad build type: " + ptype);
          }
          
          if (! canBuild)
          {
              playerInterface.print("Sorry, you cannot build there.");
              return;
          }
          
          if (isInitialPlacement || wantsCancel)
          {
              // - Easy, we've sent it right away.  Done with placing this piece.
              clearModeAndHilight(ptype);
              return;
          }

          // - During gameplay: Send, wait to receive gameState, send.
              
          // Set up timer to expect first-reply (and then send the second message)
          popupSetBuildRequest(buildLoc, ptype);

          // Now that we're expecting that, use buttons to send the first message         
          playerInterface.getBuildingPanel().clickBuildingButton
              (game, playerInterface.getClient(), btarget, true);          
      }
      
      void tryCancel()
      {
          String btarget = null;
          switch (cancelBuildType)
          {
          case SOCPlayingPiece.ROAD:
              btarget = SOCBuildingPanel.ROAD;
              break;
          case SOCPlayingPiece.SETTLEMENT:
              btarget = SOCBuildingPanel.STLMT;
              break;
          case SOCPlayingPiece.CITY:
              btarget = SOCBuildingPanel.CITY;
              break;
          }          
          // Use buttons to cancel the build request
          playerInterface.getBuildingPanel().clickBuildingButton
              (game, playerInterface.getClient(), btarget, false);
      }

    }  // inner class BoardPopupMenu    

    /**
     * Menu for right-click on 3-for-1 port to trade all resource types with bank/port.
     * Menu items won't necessarily say "trade 3", because the user may have a 2-for-1
     * port, or may not have a 3-for-1 port (cost 4).
     *
     * @author Jeremy D Monin <jeremy@nand.net>
     */
    /* package-access */ static class ResourceTradeAllMenu extends SOCHandPanel.ResourceTradePopupMenu
    {
        private SOCBoardPanel bpanel;
        private SOCHandPanel.ResourceTradeTypeMenu[] tradeFromTypes;        

        /**
         * Temporary menu for board popup menu
         *
         * @throws IllegalStateException If client not current player
         */
        public ResourceTradeAllMenu(SOCBoardPanel bp, SOCHandPanel hp)
            throws IllegalStateException
        {
            super(hp, "Trade Port");
            bpanel = bp;
            SOCPlayerInterface pi = hp.getPlayerInterface();
            if (! pi.clientIsCurrentPlayer())
                throw new IllegalStateException("Not current player");

          tradeFromTypes = new SOCHandPanel.ResourceTradeTypeMenu[5];
          for (int i = 0; i < 5; ++i)
          {
              tradeFromTypes[i] = new SOCHandPanel.ResourceTradeTypeMenu(hp, i+1, true);
              add(tradeFromTypes[i]);
          }
        }

        /**
         * Show menu at this position. Before showing, enable or
         * disable based on gamestate and player's resources.
         * 
         * @param x   Mouse x-position relative to colorsquare
         * @param y   Mouse y-position relative to colorsquare
         */
        public void show(int x, int y)
        {
            setEnabledIfCanTrade(false);
            super.show(bpanel, x, y);
        }

        /**
         * Enable or disable based on gamestate and player's resources.
         *
         * @param itemsOnly If true, enable/disable items, instead of the menu itself.
         *                  The submenus are considered items.
         *                  Items within submenus are also items. 
         */
        public void setEnabledIfCanTrade(boolean itemsOnly)
        {
            int gs = hpan.getGame().getGameState();
            for (int i = 0; i < 5; ++i)
            {
                int numNeeded = tradeFromTypes[i].getResourceCost(); 
                tradeFromTypes[i].setEnabledIfCanTrade(itemsOnly);
                tradeFromTypes[i].setEnabledIfCanTrade
                    ((gs == SOCGame.PLAY1)
                     && (numNeeded <= hpan.getPlayer().getResources().getAmount(i+1)));                    
            }
        }

        /** Cleanup, for removing this menu. */
        public void destroy()
        {
            for (int i = 0; i < 5; ++i)
            {
                if (tradeFromTypes[i] != null)
                {
                    SOCHandPanel.ResourceTradeTypeMenu mi = tradeFromTypes[i];
                    tradeFromTypes[i] = null;
                    mi.destroy();
                }
            }
            removeAll();
            hpan = null;
        }

    }  /* static nested class ResourceTradeAllMenu */

    /** 
     * Used for the delay between sending a build-request message,
     * and receiving a game-state message.
     * 
     * This timer will probably not be called, unless there's a large lag
     * between the server and client.  It's here just in case.
     * Ideally the server responds right away, and the client responds then.
     * 
     * @see SOCHandPanel#autoRollSetupTimer()
     */
    protected class BoardPanelSendBuildTask extends java.util.TimerTask
    {
        protected int buildLoc, pieceType;
        protected boolean wasSentAlready;

        /** Send this after maximum delay.
         * 
         * @param coord Board coordinates, as used in SOCPutPiece message. Does not accept -1 for road edge 0x00.
         * @param ptype Piece type, as used in SOCPlayingPiece / SOCPutPiece
         */
        protected BoardPanelSendBuildTask (int coord, int ptype)
        {
            buildLoc = coord;
            pieceType = ptype;
            wasSentAlready = false;
        }
        
        /** Board coordinates, as used in SOCPutPiece message */
        public int getBuildLoc()
        {
            return buildLoc;
        }
        
        /** Piece type, as used in SOCPlayingPiece / SOCPutPiece */
        public int getPieceType()
        {
            return pieceType;
        }
        
        /**
         * This timer will probably not be called, unless there's a large lag
         * between the server and client.  It's here just in case.
         */
        public void run()
        {
            // for debugging
            if (Thread.currentThread().getName().startsWith("Thread-"))
            {
                try {
                    Thread.currentThread().setName("timertask-boardpanel");
                }
                catch (Throwable th) {}
            }
            
            // Time is up.
            sendOnceFromClientIfCurrentPlayer();
        }

        public synchronized void doNotSend()
        {
            wasSentAlready = true;
        }

        public synchronized boolean wasItSentAlready()
        {
            return wasSentAlready;
        }

        /**
         * Internally synchronized around setSentAlready/wasItSentAlready.
         * Assumes player != null because of conditions leading to the call.
         */
        public void sendOnceFromClientIfCurrentPlayer()
        {
            synchronized (this)
            {
                if (wasItSentAlready())
                    return;
                doNotSend();  // Since we're about to send it.
            }

            // Should only get here once, in one thread.
            if (! playerInterface.clientIsCurrentPlayer())
                return;  // Stale request, player's already changed
            
            SOCPlayerClient client = playerInterface.getClient();

            switch (pieceType)
            {
            case SOCPlayingPiece.ROAD:
                if (player.isPotentialRoad(buildLoc))
                    client.putPiece(game, new SOCRoad(player, buildLoc, board));
                break;
            case SOCPlayingPiece.SETTLEMENT:
                if (player.isPotentialSettlement(buildLoc))
                    client.putPiece(game, new SOCSettlement(player, buildLoc, board));
                break;
            case SOCPlayingPiece.CITY:
                if (player.isPotentialCity(buildLoc))
                    client.putPiece(game, new SOCCity(player, buildLoc, board));
                break;
            }

            clearModeAndHilight(pieceType);           
        }
        
    }  // inner class BoardPanelSendBuildTask
    
}  // class SOCBoardPanel
