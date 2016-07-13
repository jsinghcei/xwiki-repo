/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.watchlist.internal.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.watchlist.internal.WatchListEventHTMLDiffExtractor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventType;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * The class representing an event in the WatchList. The current implementation is a wrapper for one or more
 * ActivityEvent.
 *
 * @version $Id$
 */
public class WatchListEvent implements Comparable<WatchListEvent>
{
    /**
     * Default document version on creation.
     */
    private static final String INITIAL_DOCUMENT_VERSION = "1.1";

    /**
     * The version before the initial version used for document, used to get empty versions of documents.
     */
    private static final String PREINITIAL_DOCUMENT_VERSION = "1.0";

    /**
     * Event hashcode.
     */
    private final int hashCode;

    /**
     * Type of the event (example: "update").
     */
    private String type;

    /**
     * Version of the document before the event happened.
     */
    private String previousVersion;

    /**
     * List of versions affected by this event. It will contain only one entry if the event is not a composite event.
     */
    private List<String> versions;

    /**
     * List of authors for this event. It will contain only one entry if the event is not a composite event.
     */
    private List<String> authors;

    /**
     * List of dates for this event. It will contain only one entry if the event is not a composite event.
     */
    private List<Date> dates;

    /**
     * Difference generated by update events in a document, formatted in HTML.
     */
    private String htmlDiff;

    private DocumentReference documentReference;

    private DocumentReference authorReference;

    private List<DocumentReference> authorReferences;

    private String version;

    private Date date;

    private List<WatchListEvent> events;

    private EntityReferenceSerializer<String> serializer;

    private EntityReferenceSerializer<String> localSerializer;

    /**
     * Constructor.
     *
     * @param documentReference the document on which the event happened
     * @param type the type of event
     * @param userReference the user that triggered the event
     * @param version the version of the document after the event happened on it
     * @param date the date of the event
     */
    public WatchListEvent(DocumentReference documentReference, String type, DocumentReference userReference,
        String version, Date date)
    {
        this.documentReference = documentReference;
        this.type = type;
        this.authorReference = userReference;
        this.version = version;
        this.date = date;

        this.events = new ArrayList<>();
        this.events.add(this);

        int hash = 3;
        if (ActivityEventType.UPDATE.equals(type)) {
            hashCode = 42 * hash + documentReference.hashCode() + type.hashCode();
        } else {
            hashCode = 42 * hash + documentReference.hashCode() + type.hashCode() + date.hashCode();
        }
    }

    /**
     * Add another event associated to this event.
     *
     * @param event The event to add.
     */
    public void addEvent(WatchListEvent event)
    {
        if (ActivityEventType.DELETE.equals(event.getType())) {
            // If the document has been deleted, reset this event
            type = event.getType();
            versions = null;
            authors = null;
            previousVersion = null;
            htmlDiff = null;
        } else if (ActivityEventType.UPDATE.equals(event.getType()) && ActivityEventType.DELETE.equals(getType())) {
            // If an update event had been fired before a delete, discard it
            return;
        }

        events.add(event);
    }

    /**
     * @return The wiki in which the event happened.
     */
    public String getWiki()
    {
        return getDocumentReference().getWikiReference().getName();
    }

    /**
     * @return The space in which the event happened.
     */
    public String getSpace()
    {
        SpaceReference spaceReference = getDocumentReference().getLastSpaceReference();
        return getLocalSerializer().serialize(spaceReference);
    }

    /**
     * @return The space, prefixed with the wiki name, in which the event happened (example: "xwiki:Main").
     */
    public String getPrefixedSpace()
    {
        SpaceReference spaceReference = getDocumentReference().getLastSpaceReference();
        return getSerializer().serialize(spaceReference);
    }

    /**
     * @return The fullName of the document which has generated this event (example: "Main.WebHome").
     */
    public String getFullName()
    {
        return getLocalSerializer().serialize(getDocumentReference());
    }

    /**
     * @return The fullName of the document which has generated this event, prefixed with the wiki name. Example:
     *         "xwiki:Main.WebHome".
     */
    public String getPrefixedFullName()
    {
        return getSerializer().serialize(getDocumentReference());
    }

    /**
     * @return The external URL of the document which has fired the event
     * @deprecated use the XWiki API to get the internal/external URL of the document, using
     *             {@link #getDocumentReference()}.
     */
    @Deprecated
    public String getUrl()
    {
        String url = "";

        try {
            XWikiContext context = getXWikiContext();
            url = context.getWiki().getDocument(getDocumentReference(), context).getExternalURL("view", context);
        } catch (Exception e) {
            // Do nothing, we don't want to throw exceptions in notification emails.
        }

        return url;
    }

    /**
     * @return The date when the event occurred.
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @return Get all the dates of a composite event, if this event is not a composite this list will contain single
     *         entry.
     */
    public List<Date> getDates()
    {
        if (dates == null) {
            dates = new ArrayList<Date>();

            for (WatchListEvent event : events) {
                dates.add(event.getDate());
            }
        }

        return dates;
    }

    /**
     * @return The type of this event (example: "update", "delete").
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return The user who generated the event.
     */
    public String getAuthor()
    {
        if (authorReference == null) {
            return XWikiRightService.GUEST_USER_FULLNAME;
        }

        return getSerializer().serialize(authorReference);
    }

    /**
     * @return Get all the authors of a composite event, if this event is not a composite this list will contain single
     *         entry.
     */
    public List<String> getAuthors()
    {
        if (authors == null) {
            authors = new ArrayList<String>();

            for (WatchListEvent event : events) {
                String author = event.getAuthor();
                if (!authors.contains(author)) {
                    authors.add(author);
                }
            }
        }

        return authors;
    }

    /**
     * @return the user who generated the event
     */
    public DocumentReference getAuthorReference()
    {
        return authorReference;
    }

    /**
     * @return all the references of the authors of a composite event, if this event is not a composite this list will
     *         contain single entry.
     * @since 7.1RC1
     */
    public List<DocumentReference> getAuthorReferences()
    {
        if (authorReferences == null) {
            authorReferences = new ArrayList<DocumentReference>();

            for (WatchListEvent event : events) {
                DocumentReference eventAuthorReference = event.getAuthorReference();
                if (!authorReferences.contains(eventAuthorReference)) {
                    authorReferences.add(eventAuthorReference);
                }
            }
        }

        return authorReferences;
    }

    /**
     * @return The version of the document at the time it has generated the event.
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @return All the versions from a composite event, if the event is not a composite the list will contain a single
     *         entry
     */
    public List<String> getVersions()
    {
        if (versions == null) {
            versions = new ArrayList<String>();

            for (WatchListEvent event : events) {
                if (!StringUtils.isBlank(event.getVersion()) && !versions.contains(event.getVersion())) {
                    versions.add(event.getVersion());
                }
            }
        }

        return versions;
    }

    /**
     * @return The version of the document which has generated the event, before the actual event.
     */
    public String getPreviousVersion()
    {
        if (previousVersion == null) {
            String initialVersion = "";
            previousVersion = "";

            try {
                List<String> allVersions = getVersions();
                initialVersion = allVersions.get(allVersions.size() - 1);

                if (initialVersion.equals(INITIAL_DOCUMENT_VERSION)) {
                    previousVersion = PREINITIAL_DOCUMENT_VERSION;
                } else if (!StringUtils.isBlank(initialVersion)) {
                    // Retrieve the previous version from the document archive.
                    XWikiContext context = getXWikiContext();

                    XWikiDocument doc = context.getWiki().getDocument(getDocumentReference(), context);
                    XWikiDocument initialDoc = context.getWiki().getDocument(doc, initialVersion, context);
                    String docPreviousVersion = initialDoc.getPreviousVersion();
                    if (docPreviousVersion != null) {
                        this.previousVersion = docPreviousVersion;
                    }
                }
            } catch (XWikiException e) {
                // Catch the exception to be sure we won't send emails containing stacktraces to users.
                e.printStackTrace();
            }
        }

        return previousVersion;
    }

    /**
     * @return True if the event is made of multiple events.
     */
    public boolean isComposite()
    {
        return events.size() > 1;
    }

    /**
     * @return The diff, formatted in HTML, to display to the user when a document has been updated, or null if an error
     *         occurred while computing the diff
     */
    public String getHTMLDiff()
    {
        // TODO: Deprecate this method and offer an alternative to compute it from a script service that accesses the
        // WatchListEventHTMLDiffExtractor component.
        if (htmlDiff == null) {
            try {
                htmlDiff = Utils.getComponent(WatchListEventHTMLDiffExtractor.class).getHTMLDiff(this);
            } catch (Exception e) {
                // Catch the exception to be sure we won't send emails containing stacktraces to users.
                e.printStackTrace();
            }
        }

        return htmlDiff;
    }

    /**
     * Perform a string comparison on the prefixed fullName of the source document.
     *
     * @param event event to compare with
     * @return the result of the string comparison
     */
    @Override
    public int compareTo(WatchListEvent event)
    {
        return getDocumentReference().compareTo(event.getDocumentReference());
    }

    /**
     * @return the document on which the event happened
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Overriding of the default equals method.
     *
     * @param obj the ActivityEvent to be compared with
     * @return True if the two events have been generated by the same document and are equals or conflicting
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WatchListEvent)) {
            return false;
        }

        // At first this method was returning true when the documents were the same and the events were the same type.
        // Since we don't want to keep update events for documents that have been deleted this method has been modified
        // to a point were it performs something different from a equals(), it returns true when obj is a delete event
        // and 'this' is an update event. See WatchListEventManager#WatchListEventManager(Date, XWikiContext).
        // TODO: refactoring.
        WatchListEvent event = ((WatchListEvent) obj);
        return this.documentReference.equals(event.getDocumentReference())
            && WatchListEventType.UPDATE.equals(getType())
            && (WatchListEventType.UPDATE.equals(event.getType()) || WatchListEventType.DELETE.equals(event.getType()));
    }

    private EntityReferenceSerializer<String> getSerializer()
    {
        if (this.serializer == null) {
            this.serializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        }

        return this.serializer;
    }

    private EntityReferenceSerializer<String> getLocalSerializer()
    {
        if (this.localSerializer == null) {
            this.localSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }

        return this.localSerializer;
    }

    private XWikiContext getXWikiContext()
    {
        Provider<XWikiContext> provider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);
        return provider.get();
    }
}
