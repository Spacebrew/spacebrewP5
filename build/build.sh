BASEDIR=$(cd "$(dirname "$0")"; pwd)
VERSION_NUMBER=4

# Parse options
if [ ${#} == 1  ] ; then
	VERSION_NUMBER=$1
fi

cd library/spacebrew/resources
ant
cd ../
ls
cp distribution/spacebrew-$VERSION_NUMBER/download/spacebrew.zip ../../dist/spacebrew.zip
cd ../../dist
rm spacebrew
mkdir spacebrew
unzip spacebrew.zip -d ./spacebrew
rm spacebrew.zip