/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2007,2008 Jeremy D Monin <jeremy@nand.net>
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

import soc.game.SOCGame;


/**
 * This is the dialog to confirm when someone clicks the Quit Game button.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
class SOCQuitConfirmDialog extends AskDialog
{
    /**
     * Creates and shows a new SOCQuitConfirmDialog.
     * If the game is over, the "Quit" button is the default;
     * otherwise, Continue is default.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     * @throws IllegalArgumentException If cli or gamePI is null
     */
    public static void createAndShow(SOCPlayerClient cli, SOCPlayerInterface gamePI)
        throws IllegalArgumentException
    {
        if ((cli == null) || (gamePI == null))
            throw new IllegalArgumentException("no nulls");
        SOCGame ga = gamePI.getGame();
        boolean gaOver = (ga.getGameState() >= SOCGame.OVER);

        SOCQuitConfirmDialog qcd = new SOCQuitConfirmDialog(cli, gamePI, gaOver);
        qcd.show();      
    }
    

    /**
     * Creates a new SOCQuitConfirmDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     * @param gameIsOver The game is over - "Quit" button should be default (if not over, Continue is default)
     */
    protected SOCQuitConfirmDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI, boolean gameIsOver)
    {
        super(cli, gamePI, "Really quit game "
                + gamePI.getGame().getName() + "?",
            (gameIsOver
                ? "Do you want to quit this finished game?"
                : "Do you want to quit the game being played?"),
            "Quit this game", 
            (gameIsOver
                ? "Don't quit"
                : "Continue playing"),
            ((gamePI.getGame().getGameState() != SOCGame.NEW)
                ? "Reset board"
                : null),
            (gameIsOver ? 1 : 2));
    }

    /**
     * React to the Quit button. (call playerInterface.leaveGame)
     */
    public void button1Chosen()
    {
        pi.leaveGame();
    }

    /**
     * React to the Continue button. (Nothing to do)
     */
    public void button2Chosen()
    {
        // Nothing to do (continue playing)
    }

    /**
     * React to the Reset Board button. (call playerInterface.resetBoardRequest)
     */
    public void button3Chosen()
    {
        pi.resetBoardRequest();
    }

    /**
     * React to the dialog window closed by user. (Nothing to do)
     */
    public void windowCloseChosen()
    {
        // Nothing to do (continue playing)
    }

}
