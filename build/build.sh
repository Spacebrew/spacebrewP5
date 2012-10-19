BASEDIR=$(cd "$(dirname "$0")"; pwd)
VERSION_NUMBER=1

# Parse options
if [ ${#} == 1  ] ; then
	VERSION_NUMBER=$1
fi

cd library/spacebrew/resources
ant
cd ../
ls
cp distribution/Spacebrew-$VERSION_NUMBER/download/Spacebrew.zip ../../dist/Spacebrew.zip
cd ../../dist
rm Spacebrew
mkdir Spacebrew
unzip Spacebrew.zip -d ./Spacebrew
rm Spacebrew.zip