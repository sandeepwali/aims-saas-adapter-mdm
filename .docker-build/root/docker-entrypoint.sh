#!/usr/bin/env bash

#
# Banner
#

echo '
--------------------------------------------------------------------
           _____ __  __  _____    _____           _
     /\   |_   _|  \/  |/ ____|  / ____|         | |
    /  \    | | | \  / | (___   | (___  _   _ ___| |_ ___ _ __ ___
   / /\ \   | | | |\/| |\___ \   \___ \| | | / __| __/ _ \  _ ` _ \
  / ____ \ _| |_| |  | |____) |  ____) | |_| \__ \ ||  __/ | | | | |
 /_/    \_\_____|_|  |_|_____/  |_____/ \__, |___/\__\___|_| |_| |_|
                                         __/ |
                                        |___/
Brought to you by solumesl.com

'

echo "
--------------------------------------------------------------------------
Unprivileged user
--------------------------------------------------------------------------
User name:   aims
User uid:    $(id -u aims)
User gid:    $(id -g aims)

"

#
# Functions
#

replace_with_old_variable_value ()
{
    #
    # Description:
    #   Replace old variable value with new empty variable value
    #
    # Usage:
    #   replace_with_old_variable_value old_variable new_variable
    #
    # Example:
    #   DATABASE_URL="jdbc:postgresql://127.0.0.1:6010/AIMS_PORTAL_DB"
    #   AIMS_CLIENT_DATABASE_URL=
    #   replace_with_old_variable_value DATABASE_URL AIMS_CLIENT_DATABASE_URL
    #

    old_variable=$1
    new_variable=$2

    # Expand old_variable value into old_variable_value
    eval "old_variable_value=\$$old_variable"

    if [ -n "$old_variable_value" ]
    then
        # Old variable value is not empty, show depreciated message
        echo "Warning: \$${old_variable} is depreciated use \$${new_variable}"

        # Check if new variable is empty
        eval "new_variable_value=\$$new_variable"
        if [ -z "$new_variable_value" ]
        then
            echo "New variable \$${new_variable} is empty..."
            echo "Replacing \$${old_variable} with \$${new_variable}"
            eval "$new_variable=$old_variable_value"
        fi
    fi

}

replace_empty_variable_with_default_value ()
{
    #
    # Description:
    #   Replace empty variable with default value
    #
    # Usage:
    #   replace_empty_variable_with_default_value variable_name default_value
    #
    # Example:
    #   AIMS_CLIENT_DATABASE_URL=""
    #   replace_empty_variable_with_default_value AIMS_CLIENT_DATABASE_URL jdbc:postgresql://127.0.0.1:6010/AIMS_PORTAL_DB
    #   echo $AIMS_CLIENT_DATABASE_URL

    variable_name=$1
    default_value=$2

    # Expand variable value into variable_value
    eval "variable_value=\$$variable_name"

    if [ -z "$variable_value" ]
    then
        echo "Variable \$$variable_name is empty using default value $default_value"
        export ${variable_name}=${default_value}
    fi
}



replace_property ()
{
    property_name=$1
    property_value=$2

    sed -i -e "s|${property_name}=.*|${property_name}=${property_value}|" "/app/application.properties"
}

#
# Variables
#

## Use old variable value if new variable is not provided
## Usage: replace_with_old_variable_value <old_variable> <new_variable>
#replace_with_old_variable_value SERVER_PORT AIMS_CLIENT_SERVER_PORT


## Set empty variables with default value
## Usage: replace_empty_variable_with_default_value <variable_name> <variable_value>
replace_empty_variable_with_default_value JAVA_XMS "1g"
replace_empty_variable_with_default_value JAVA_XMX "1g"

replace_empty_variable_with_default_value SERVER_PORT "8080"

replace_empty_variable_with_default_value LOGGING_LEVEL_COM_SOLUM "INFO"


echo "
--------------------------------------------------------------------------
AIMS Client
--------------------------------------------------------------------------
$(env)
"

#
# Exec Java Application
#

exec /opt/java/openjdk/bin/java ${JAVA_OPTS} \
    -Xms${JAVA_XMS} \
    -Xmx${JAVA_XMX} \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /app/aims-saas-adapter-mdm.jar;