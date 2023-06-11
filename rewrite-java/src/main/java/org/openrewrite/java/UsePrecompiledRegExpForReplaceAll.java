package org.openrewrite.java;

import static org.openrewrite.Tree.randomId;

import java.util.regex.Pattern;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

public class UsePrecompiledRegExpForReplaceAll
   extends Recipe
{

   public static final String REG_EXP_KEY = "regExp";

   @Override
   public String getDisplayName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return getClass().getSimpleName() + ".";
   }

   @Override
   public TreeVisitor< ?, ExecutionContext > getVisitor()
   {
      return new ReplaceVisitor();
   }

   public static class ReplaceVisitor
      extends JavaIsoVisitor< ExecutionContext >
   {
      private static final String PATTERN_VAR = "openRewriteReplaceAllPatternVar";
      JavaTemplate matcherTemplate =
         JavaTemplate.builder( "#{any()}.matcher( #{any(java.lang.CharSequence)}).replaceAll( #{any(java.lang.String)})" )
                     .contextSensitive()
                     .imports( "java.util.regex.Pattern" )
                     .build();
      JavaTemplate compilePatternTemplate =
         JavaTemplate.builder( "private static final java.util.regex.Pattern " + PATTERN_VAR + "= Pattern.compile(#{});" )
                     .contextSensitive()
                     .imports( "java.util.regex.Pattern" )
                     .build();

      @Override
      public J.Block visitBlock(
         final J.Block block,
         final ExecutionContext executionContext )
      {
         final J.Block block1 = super.visitBlock( block, executionContext );
         final Cursor parentOrThrow = getCursor().getParentOrThrow();
         if( parentOrThrow.getValue() instanceof J.ClassDeclaration ) {
            Object regExp = parentOrThrow.pollNearestMessage( REG_EXP_KEY );
            if( regExp == null ) {
               return block1;
            }
            return super.visitBlock( compilePatternTemplate.apply( getCursor(),
                                                                   block1.getCoordinates().firstStatement(),
                                                                   regExp ), executionContext );
         }
         return block1;
      }

      @Override
      public J.MethodInvocation visitMethodInvocation(
         J.MethodInvocation method,
         ExecutionContext executionContext )
      {
         J.MethodInvocation invocation = super.visitMethodInvocation( method, executionContext );
         JavaType.Method methodType = invocation.getMethodType();
         if( methodType == null
             || !"java.lang.String".equals( methodType.getDeclaringType().getFullyQualifiedName() )
             || !"replaceAll".equals( invocation.getName().getSimpleName() ) ) {
            return invocation;
         }
         Object varIdentifier = new J.Identifier( randomId(),
                                                  Space.SINGLE_SPACE,
                                                  Markers.EMPTY,
                                                  PATTERN_VAR,
                                                  JavaType.buildType( Pattern.class.getName() ),
                                                  null );
         getCursor().putMessageOnFirstEnclosing( J.ClassDeclaration.class, REG_EXP_KEY, invocation.getArguments().get( 0 ) );
         return matcherTemplate.apply( getCursor(),
                                       invocation.getCoordinates().replace(),
                                       varIdentifier,
                                       invocation.getSelect(),
                                       invocation.getArguments().get( 1 ) );
      }
   }
}
