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

package org.osgi.service.metatype.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.metatype.MetaTypeService;

/**
 * This annotation can be used to require the Meta Type implementation. It can
 * be used directly, or as a meta-annotation.
 * 
 * @author $Id: 02c5858b2c32c78cf3ca2dabd9a25ccae49b1686 $
 * @since 1.4
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.TYPE, ElementType.PACKAGE
})
@Requirement(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
		name = MetaTypeService.METATYPE_CAPABILITY_NAME, //
		version = MetaTypeService.METATYPE_SPECIFICATION_VERSION)
public @interface RequireMetaTypeImplementation {
	// This is a marker annotation.
}
