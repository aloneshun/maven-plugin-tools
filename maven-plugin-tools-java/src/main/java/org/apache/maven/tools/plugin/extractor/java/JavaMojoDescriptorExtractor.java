package org.apache.maven.tools.plugin.extractor.java;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.Type;

import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Extracts Mojo descriptors from <a href="http://java.sun.com/">Java</a> sources.
 * <br/>
 * For more information about the usage tag, have a look to:
 * <a href="http://maven.apache.org/developers/mojo-api-specification.html">http://maven.apache.org/developers/mojo-api-specification.html</a>
 *
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 * @version $Id$
 * @see org.apache.maven.plugin.descriptor.MojoDescriptor
 */
public class JavaMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor, JavaMojoAnnotation
{
    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#INSTANTIATION_STRATEGY} instead of. */
    public static final String MAVEN_PLUGIN_INSTANTIATION = JavaMojoAnnotation.INSTANTIATION_STRATEGY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#CONFIGURATOR} instead of. */
    public static final String CONFIGURATOR = JavaMojoAnnotation.CONFIGURATOR;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER} instead of. */
    public static final String PARAMETER = JavaMojoAnnotation.PARAMETER;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_EXPRESSION} instead of. */
    public static final String PARAMETER_EXPRESSION = JavaMojoAnnotation.PARAMETER_EXPRESSION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_DEFAULT_VALUE} instead of. */
    public static final String PARAMETER_DEFAULT_VALUE = JavaMojoAnnotation.PARAMETER_DEFAULT_VALUE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_ALIAS} instead of. */
    public static final String PARAMETER_ALIAS = JavaMojoAnnotation.PARAMETER_ALIAS;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#SINCE} instead of. */
    public static final String SINCE = JavaMojoAnnotation.SINCE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_IMPLEMENTATION} instead of. */
    public static final String PARAMETER_IMPLEMENTATION = JavaMojoAnnotation.PARAMETER_IMPLEMENTATION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PARAMETER_PROPERTY} instead of. */
    public static final String PARAMETER_PROPERTY = JavaMojoAnnotation.PARAMETER_PROPERTY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRED} instead of. */
    public static final String REQUIRED = JavaMojoAnnotation.REQUIRED;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#DEPRECATED} instead of. */
    public static final String DEPRECATED = JavaMojoAnnotation.DEPRECATED;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#READONLY} instead of. */
    public static final String READONLY = JavaMojoAnnotation.READONLY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#GOAL} instead of. */
    public static final String GOAL = JavaMojoAnnotation.GOAL;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#PHASE} instead of. */
    public static final String PHASE = JavaMojoAnnotation.PHASE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE} instead of. */
    public static final String EXECUTE = JavaMojoAnnotation.EXECUTE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_LIFECYCLE} instead of. */
    public static final String EXECUTE_LIFECYCLE = JavaMojoAnnotation.EXECUTE_LIFECYCLE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_PHASE} instead of. */
    public static final String EXECUTE_PHASE = JavaMojoAnnotation.EXECUTE_PHASE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#EXECUTE_GOAL} instead of. */
    public static final String EXECUTE_GOAL = JavaMojoAnnotation.EXECUTE_GOAL;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#DESCRIPTION} instead of. */
    public static final String GOAL_DESCRIPTION = JavaMojoAnnotation.DESCRIPTION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_DEPENDENCY_RESOLUTION} instead of. */
    public static final String GOAL_REQUIRES_DEPENDENCY_RESOLUTION = JavaMojoAnnotation.REQUIRES_DEPENDENCY_RESOLUTION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_PROJECT} instead of. */
    public static final String GOAL_REQUIRES_PROJECT = JavaMojoAnnotation.REQUIRES_PROJECT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_REPORTS} instead of. */
    public static final String GOAL_REQUIRES_REPORTS = JavaMojoAnnotation.REQUIRES_REPORTS;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#AGGREGATOR} instead of. */
    public static final String GOAL_IS_AGGREGATOR = JavaMojoAnnotation.AGGREGATOR;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_ONLINE} instead of. */
    public static final String GOAL_REQUIRES_ONLINE = JavaMojoAnnotation.REQUIRES_ONLINE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#INHERIT_BY_DEFAULT} instead of. */
    public static final String GOAL_INHERIT_BY_DEFAULT = JavaMojoAnnotation.INHERIT_BY_DEFAULT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#MULTI_EXECUTION_STRATEGY} instead of. */
    public static final String GOAL_MULTI_EXECUTION_STRATEGY = JavaMojoAnnotation.MULTI_EXECUTION_STRATEGY;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#REQUIRES_DIRECT_INVOCATION} instead of. */
    public static final String GOAL_REQUIRES_DIRECT_INVOCATION = JavaMojoAnnotation.REQUIRES_DIRECT_INVOCATION;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT} instead of. */
    public static final String COMPONENT = JavaMojoAnnotation.COMPONENT;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT_ROLE} instead of. */
    public static final String COMPONENT_ROLE = JavaMojoAnnotation.COMPONENT_ROLE;

    /** @deprecated since 2.4, use {@link JavaMojoAnnotation#COMPONENT_ROLEHINT} instead of. */
    public static final String COMPONENT_ROLEHINT = JavaMojoAnnotation.COMPONENT_ROLEHINT;

    /**
     * @param parameter not null
     * @param i
     * @throws InvalidParameterException if any
     */
    protected void validateParameter( Parameter parameter, int i )
        throws InvalidParameterException
    {
        // TODO: remove when backward compatibility is no longer an issue.
        String name = parameter.getName();

        if ( name == null )
        {
            throw new InvalidParameterException( "name", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
        String type = parameter.getType();

        if ( type == null )
        {
            throw new InvalidParameterException( "type", i );
        }

        // TODO: remove when backward compatibility is no longer an issue.
        String description = parameter.getDescription();

        if ( description == null )
        {
            throw new InvalidParameterException( "description", i );
        }
    }

    // ----------------------------------------------------------------------
    // Mojo descriptor creation from @tags
    // ----------------------------------------------------------------------

    /**
     * @param javaClass not null
     * @return a mojo descriptor
     * @throws InvalidPluginDescriptorException if any
     */
    protected MojoDescriptor createMojoDescriptor( JavaClass javaClass )
        throws InvalidPluginDescriptorException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();

        mojoDescriptor.setLanguage( "java" );

        mojoDescriptor.setImplementation( javaClass.getFullyQualifiedName() );

        mojoDescriptor.setDescription( javaClass.getComment() );

        DocletTag tag = findInClassHierarchy( javaClass, JavaMojoAnnotation.INSTANTIATION_STRATEGY );

        if ( tag != null )
        {
            mojoDescriptor.setInstantiationStrategy( tag.getValue() );
        }

        tag = findInClassHierarchy( javaClass, JavaMojoAnnotation.MULTI_EXECUTION_STRATEGY );

        if ( tag != null )
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.MULTI_PASS_EXEC_STRATEGY );
        }
        else
        {
            mojoDescriptor.setExecutionStrategy( MojoDescriptor.SINGLE_PASS_EXEC_STRATEGY );
        }

        // ----------------------------------------------------------------------
        // Configurator hint
        // ----------------------------------------------------------------------

        DocletTag configurator = findInClassHierarchy( javaClass, JavaMojoAnnotation.CONFIGURATOR );

        if ( configurator != null )
        {
            mojoDescriptor.setComponentConfigurator( configurator.getValue() );
        }

        // ----------------------------------------------------------------------
        // Goal name
        // ----------------------------------------------------------------------

        DocletTag goal = findInClassHierarchy( javaClass, JavaMojoAnnotation.GOAL );

        if ( goal != null )
        {
            mojoDescriptor.setGoal( goal.getValue() );
        }

        // ----------------------------------------------------------------------
        // What version it was introduced in
        // ----------------------------------------------------------------------

        DocletTag since = findInClassHierarchy( javaClass, JavaMojoAnnotation.SINCE );

        if ( since != null )
        {
            mojoDescriptor.setSince( since.getValue() );
        }

        // ----------------------------------------------------------------------
        // Deprecation hint
        // ----------------------------------------------------------------------

        DocletTag deprecated = javaClass.getTagByName( JavaMojoAnnotation.DEPRECATED );

        if ( deprecated != null )
        {
            mojoDescriptor.setDeprecated( deprecated.getValue() );
        }

        // ----------------------------------------------------------------------
        // Phase name
        // ----------------------------------------------------------------------

        DocletTag phase = findInClassHierarchy( javaClass, JavaMojoAnnotation.PHASE );

        if ( phase != null )
        {
            mojoDescriptor.setPhase( phase.getValue() );
        }

        // ----------------------------------------------------------------------
        // Additional phase to execute first
        // ----------------------------------------------------------------------

        DocletTag execute = findInClassHierarchy( javaClass, JavaMojoAnnotation.EXECUTE );

        if ( execute != null )
        {
            String executePhase = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_PHASE );
            String executeGoal = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_GOAL );

            if ( executePhase == null && executeGoal == null )
            {
                throw new InvalidPluginDescriptorException( "@execute tag requires a 'phase' or 'goal' parameter" );
            }
            else if ( executePhase != null && executeGoal != null )
            {
                throw new InvalidPluginDescriptorException(
                    "@execute tag can have only one of a 'phase' or 'goal' parameter" );
            }
            mojoDescriptor.setExecutePhase( executePhase );
            mojoDescriptor.setExecuteGoal( executeGoal );

            String lifecycle = execute.getNamedParameter( JavaMojoAnnotation.EXECUTE_LIFECYCLE );

            if ( lifecycle != null )
            {
                mojoDescriptor.setExecuteLifecycle( lifecycle );
                if ( mojoDescriptor.getExecuteGoal() != null )
                {
                    throw new InvalidPluginDescriptorException(
                        "@execute lifecycle requires a phase instead of a goal" );
                }
            }
        }

        // ----------------------------------------------------------------------
        // Dependency resolution flag
        // ----------------------------------------------------------------------

        DocletTag requiresDependencyResolution = findInClassHierarchy( javaClass, JavaMojoAnnotation.REQUIRES_DEPENDENCY_RESOLUTION );

        if ( requiresDependencyResolution != null )
        {
            String value = requiresDependencyResolution.getValue();

            if ( value == null || value.length() == 0 )
            {
                value = "runtime";
            }

            mojoDescriptor.setDependencyResolutionRequired( value );
        }

        // ----------------------------------------------------------------------
        // Project flag
        // ----------------------------------------------------------------------

        boolean value = getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_PROJECT, mojoDescriptor.isProjectRequired() );
        mojoDescriptor.setProjectRequired( value );

        // ----------------------------------------------------------------------
        // Aggregator flag
        // ----------------------------------------------------------------------

        DocletTag aggregator = findInClassHierarchy( javaClass, JavaMojoAnnotation.AGGREGATOR );

        if ( aggregator != null )
        {
            mojoDescriptor.setAggregator( true );
        }

        // ----------------------------------------------------------------------
        // requiresDirectInvocation flag
        // ----------------------------------------------------------------------

        value =
            getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_DIRECT_INVOCATION, mojoDescriptor.isDirectInvocationOnly() );
        mojoDescriptor.setDirectInvocationOnly( value );

        // ----------------------------------------------------------------------
        // Online flag
        // ----------------------------------------------------------------------

        value = getBooleanTagValue( javaClass, JavaMojoAnnotation.REQUIRES_ONLINE, mojoDescriptor.isOnlineRequired() );
        mojoDescriptor.setOnlineRequired( value );

        // ----------------------------------------------------------------------
        // inheritByDefault flag
        // ----------------------------------------------------------------------

        value = getBooleanTagValue( javaClass, JavaMojoAnnotation.INHERIT_BY_DEFAULT, mojoDescriptor.isInheritedByDefault() );
        mojoDescriptor.setInheritedByDefault( value );

        extractParameters( mojoDescriptor, javaClass );

        return mojoDescriptor;
    }

    private static boolean getBooleanTagValue( JavaClass javaClass, String tagName, boolean defaultValue )
    {
        DocletTag requiresProject = findInClassHierarchy( javaClass, tagName );

        if ( requiresProject != null )
        {
            String requiresProjectValue = requiresProject.getValue();

            if ( requiresProjectValue != null && requiresProjectValue.length() > 0 )
            {
                defaultValue = Boolean.valueOf( requiresProjectValue ).booleanValue();
            }
        }
        return defaultValue;
    }

    private static DocletTag findInClassHierarchy( JavaClass javaClass, String tagName )
    {
        DocletTag tag = javaClass.getTagByName( tagName );

        if ( tag == null )
        {
            JavaClass superClass = javaClass.getSuperJavaClass();

            if ( superClass != null )
            {
                tag = findInClassHierarchy( superClass, tagName );
            }
        }

        return tag;
    }

    private void extractParameters( MojoDescriptor mojoDescriptor, JavaClass javaClass )
        throws InvalidPluginDescriptorException
    {
        // ---------------------------------------------------------------------------------
        // We're resolving class-level, ancestor-class-field, local-class-field order here.
        // ---------------------------------------------------------------------------------

        Map rawParams = extractFieldParameterTags( javaClass );

        for ( Iterator it = rawParams.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            JavaField field = (JavaField) entry.getValue();

            Type type = field.getType();

            Parameter pd = new Parameter();

            if ( !type.isArray() )
            {
                pd.setType( type.getValue() );
            }
            else
            {
                StringBuffer value = new StringBuffer( type.getValue() );

                int remaining = type.getDimensions();

                while ( remaining-- > 0 )
                {
                    value.append( "[]" );
                }

                pd.setType( value.toString() );
            }

            pd.setDescription( field.getComment() );

            DocletTag componentTag = field.getTagByName( JavaMojoAnnotation.COMPONENT );

            if ( componentTag != null )
            {
                String role = componentTag.getNamedParameter( JavaMojoAnnotation.COMPONENT_ROLE );

                if ( role == null )
                {
                    role = field.getType().toString();
                }

                String roleHint = componentTag.getNamedParameter( JavaMojoAnnotation.COMPONENT_ROLEHINT );

                if ( roleHint == null )
                {
                    // support alternate syntax for better compatibility with the Plexus CDC.
                    roleHint = componentTag.getNamedParameter( "role-hint" );
                }

                pd.setRequirement( new Requirement( role, roleHint ) );

                pd.setName( (String) entry.getKey() );

                pd.setEditable( false );
                /* TODO: or better like this? Need @component fields be editable for the user?
                pd.setEditable( field.getTagByName( READONLY ) == null );
                */
            }
            else
            {
                DocletTag parameter = field.getTagByName( JavaMojoAnnotation.PARAMETER );

                // ----------------------------------------------------------------------
                // We will look for a property name here first and use that if present
                // i.e:
                //
                // @parameter property="project"
                //
                // Which will become the name used for the configuration element which
                // will in turn will allow plexus to use the corresponding setter.
                // ----------------------------------------------------------------------

                String property = parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_PROPERTY );

                if ( !StringUtils.isEmpty( property ) )
                {
                    pd.setName( property );
                }
                else
                {
                    pd.setName( (String) entry.getKey() );
                }

                pd.setRequired( field.getTagByName( JavaMojoAnnotation.REQUIRED ) != null );

                pd.setEditable( field.getTagByName( JavaMojoAnnotation.READONLY ) == null );

                DocletTag deprecationTag = field.getTagByName( JavaMojoAnnotation.DEPRECATED );

                if ( deprecationTag != null )
                {
                    pd.setDeprecated( deprecationTag.getValue() );
                }

                DocletTag sinceTag = field.getTagByName( JavaMojoAnnotation.SINCE );
                if ( sinceTag != null )
                {
                    pd.setSince( sinceTag.getValue() );
                }

                String alias = parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_ALIAS );

                if ( !StringUtils.isEmpty( alias ) )
                {
                    pd.setAlias( alias );
                }

                pd.setExpression( parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_EXPRESSION ) );

                if ( "${reports}".equals( pd.getExpression() ) )
                {
                    mojoDescriptor.setRequiresReports( true );
                }

                pd.setDefaultValue( parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_DEFAULT_VALUE ) );

                pd.setImplementation( parameter.getNamedParameter( JavaMojoAnnotation.PARAMETER_IMPLEMENTATION ) );
            }

            mojoDescriptor.addParameter( pd );
        }
    }

    private Map extractFieldParameterTags( JavaClass javaClass )
    {
        Map rawParams;

        // we have to add the parent fields first, so that they will be overwritten by the local fields if
        // that actually happens...
        JavaClass superClass = javaClass.getSuperJavaClass();

        if ( superClass != null )
        {
            rawParams = extractFieldParameterTags( superClass );
        }
        else
        {
            rawParams = new TreeMap();
        }

        JavaField[] classFields = javaClass.getFields();

        if ( classFields != null )
        {
            for ( int i = 0; i < classFields.length; i++ )
            {
                JavaField field = classFields[i];

                if ( field.getTagByName( JavaMojoAnnotation.PARAMETER ) != null || field.getTagByName( JavaMojoAnnotation.COMPONENT ) != null )
                {
                    rawParams.put( field.getName(), field );
                }
            }
        }
        return rawParams;
    }

    /** {@inheritDoc} */
    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        JavaClass[] javaClasses = discoverClasses( project );

        List descriptors = new ArrayList();

        for ( int i = 0; i < javaClasses.length; i++ )
        {
            DocletTag tag = javaClasses[i].getTagByName( GOAL );

            if ( tag != null )
            {
                MojoDescriptor mojoDescriptor = createMojoDescriptor( javaClasses[i] );
                mojoDescriptor.setPluginDescriptor( pluginDescriptor );

                // Validate the descriptor as best we can before allowing it to be processed.
                validate( mojoDescriptor );

                descriptors.add( mojoDescriptor );
            }
        }

        return descriptors;
    }

    /**
     * @param project not null
     * @return an array of java class
     */
    protected JavaClass[] discoverClasses( final MavenProject project )
    {
        JavaDocBuilder builder = new JavaDocBuilder();

        for ( Iterator i = project.getCompileSourceRoots().iterator(); i.hasNext(); )
        {
            builder.addSourceTree( new File( (String) i.next() ) );
        }

        // TODO be more dynamic
        if ( !project.getCompileSourceRoots()
            .contains( new File( project.getBasedir(), "target/generated-sources/plugin" ).getAbsolutePath() ) )
        {
            builder.addSourceTree( new File( project.getBasedir(), "target/generated-sources/plugin" ) );
        }

        return builder.getClasses();
    }

    /**
     * @param mojoDescriptor not null
     * @throws InvalidParameterException if any
     */
    protected void validate( MojoDescriptor mojoDescriptor )
        throws InvalidParameterException
    {
        List parameters = mojoDescriptor.getParameters();

        if ( parameters != null )
        {
            for ( int j = 0; j < parameters.size(); j++ )
            {
                validateParameter( (Parameter) parameters.get( j ), j );
            }
        }
    }
}
