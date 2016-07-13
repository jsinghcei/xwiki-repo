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
package org.xwiki.icon.internal;

import org.junit.Test;
import org.xwiki.icon.IconSet;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link org.xwiki.icon.IconSet}.
 *
 * @since 6.3RC1
 * @version $Id$
 */
public class IconSetTest
{
    @Test
    public void simpleTest() throws Exception
    {
        IconSet iconSet = new IconSet("myIconSet");
        assertEquals("myIconSet", iconSet.getName());
        iconSet.setName("newName");
        assertEquals("newName", iconSet.getName());
    }
}
