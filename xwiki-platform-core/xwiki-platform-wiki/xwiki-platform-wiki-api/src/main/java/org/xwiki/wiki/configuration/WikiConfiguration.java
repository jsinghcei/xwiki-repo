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
package org.xwiki.wiki.configuration;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration options for the Wiki module.
 *
 * @version $Id$
 * @since 5.4.4
 */
@Role
public interface WikiConfiguration
{
    /**
     * @return the suggested suffix to append to the alias of a new wiki, in the wiki creation wizard,
     * when path mode is used.
     */
    String getAliasSuffix();

    /**
     * @return if XWiki should create the database (and/or schema/user depending on the DB) for the new wiki or not.
     *         Default is true.
     * @since 14.9RC1
     */
    @Unstable
    default boolean shouldCreateDatabase()
    {
        return true;
    }
}
