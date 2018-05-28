/*
 * Copyright (c) OSGi Alliance (2018). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Event Admin Component Property Types Package Version 1.4.
 * <p>
 * When used as annotations, component property types are processed by tools to
 * generate Component Descriptions which are used at runtime.
 * <p>
 * Bundles wishing to use this package at runtime must list the package in the
 * Import-Package header of the bundle's manifest.
 * <p>
 * Example import for consumers using the API in this package:
 * <p>
 * {@code  Import-Package: org.osgi.service.event.propertytypes; version="[1.4,2.0)"}
 * 
 * @author $Id: 6af9f30b4a6831baeec375b41f5dd41132a247b6 $
 */

@Version(EVENT_ADMIN_SPECIFICATION_VERSION)
package org.osgi.service.event.propertytypes;

import static org.osgi.service.event.EventConstants.EVENT_ADMIN_SPECIFICATION_VERSION;

import org.osgi.annotation.versioning.Version;

