/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.limegroup.gnutella.gui.search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPopupMenu;

import com.frostwire.search.youtube2.YouTubeCrawledSearchResult;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.util.PopupUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class YouTubeUISearchResult extends AbstractUISearchResult {

    private final YouTubeCrawledSearchResult sr;
    private final int seeds;

    public YouTubeUISearchResult(YouTubeCrawledSearchResult sr, SearchEngine se, String query) {
        super(sr, se, query);
        this.sr = sr;

        seeds = sr.getDisplayName().indexOf(YouTubeCrawledSearchResult.AAC_HIGH_QUALITY) == 0 ? 2000 : 1000;
    }

    @Override
    public void download(boolean partial) {
        GUIMediator.instance().openYouTubeVideoUrl(sr.getDetailsUrl());
        showDetails(false);
    }

    @Override
    public JPopupMenu createMenu(JPopupMenu popupMenu, SearchResultDataLine[] lines, SearchResultMediator rp) {
        PopupUtils.addMenuItem(SearchMediator.DOWNLOAD_STRING, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                download(false);
            }
        }, popupMenu, lines.length > 0, 1);
        PopupUtils.addMenuItem(SearchMediator.YOUTUBE_DETAILS_STRING, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDetails(true);
            }
        }, popupMenu, lines.length == 1, 2);

        return popupMenu;
    }

    @Override
    public String getHash() {
        return null;
    }

    @Override
    public int getSeeds() {
        return seeds;
    }
}