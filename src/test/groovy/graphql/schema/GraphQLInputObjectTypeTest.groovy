package graphql.schema

import graphql.GraphQLContext
import graphql.StarWarsSchema
import graphql.language.ObjectValue
import graphql.validation.ValidationUtil
import spock.lang.Specification

import static graphql.Scalars.GraphQLBoolean
import static graphql.Scalars.GraphQLInt
import static graphql.Scalars.GraphQLString
import static graphql.schema.GraphQLInputObjectField.newInputObjectField
import static graphql.schema.GraphQLInputObjectType.newInputObject
import static graphql.schema.GraphQLNonNull.nonNull

class GraphQLInputObjectTypeTest extends Specification {

    def "duplicate field definition overwrites"() {
        when:
        def inputObjectType = newInputObject().name("TestInputObjectType")
                .field(newInputObjectField().name("NAME").type(GraphQLString))
                .field(newInputObjectField().name("NAME").type(GraphQLInt))
                .build()
        then:
        inputObjectType.getName() == "TestInputObjectType"
        inputObjectType.getFieldDefinition("NAME").getType() == GraphQLInt
    }

    def "builder can change existing object into a new one"() {
        given:
        def inputObjectType = newInputObject().name("StartType")
                .description("StartingDescription")
                .field(newInputObjectField().name("Str").type(GraphQLString))
                .field(newInputObjectField().name("Int").type(GraphQLInt))
                .build()

        when:
        def transformedInputType = inputObjectType.transform({ builder ->
            builder
                    .name("NewObjectName")
                    .description("NewDescription")
                    .field(newInputObjectField().name("AddedInt").type(GraphQLInt)) // add more
                    .field(newInputObjectField().name("Int").type(GraphQLInt)) // override and change
                    .field(newInputObjectField().name("Str").type(GraphQLBoolean)) // override and change
        })
        then:

        inputObjectType.getName() == "StartType"
        inputObjectType.getDescription() == "StartingDescription"
        inputObjectType.getFieldDefinitions().size() == 2
        inputObjectType.getFieldDefinition("Int").getType() == GraphQLInt
        inputObjectType.getFieldDefinition("Str").getType() == GraphQLString

        transformedInputType.getName() == "NewObjectName"
        transformedInputType.getDescription() == "NewDescription"
        transformedInputType.getFieldDefinitions().size() == 3
        transformedInputType.getFieldDefinition("AddedInt").getType() == GraphQLInt
        transformedInputType.getFieldDefinition("Int").getType() == GraphQLInt
        transformedInputType.getFieldDefinition("Str").getType() == GraphQLBoolean
    }

    def "deprecated default value builder works"() {
        given:
        def graphQLContext = GraphQLContext.getDefault()
        def schema = GraphQLSchema.newSchema().query(StarWarsSchema.queryType).build()
        def validationUtil = new ValidationUtil()
        def inputObjectType = GraphQLInputObjectType.newInputObject()
                .name("inputObjectType")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("hello")
                        .type(nonNull(GraphQLString))
                        .defaultValue("default")) // Retain deprecated builder for test coverage
                .build()
        def objectValue = ObjectValue.newObjectValue()

        expect:
        validationUtil.isValidLiteralValue(objectValue.build(), inputObjectType, schema, graphQLContext, Locale.ENGLISH)
    }
}
