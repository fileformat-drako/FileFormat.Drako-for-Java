# FileFormat.Drako


FileFormat.Drako was ported from [Google Draco](https://github.com/google/draco).


### 1. Installation

Add the following code in your pom.xml:
```
<dependency>
  <groupId>dev.fileformat</groupId>
  <artifactId>drako</artifactId>
  <version>1.4.1</version>
</dependency>
```


### 2. Example

```csharp
Vector3[] controlPoints = new Vector3[] {
    new Vector3(-5F, 0F, 5.0f),
    new Vector3(5F, 0F, 5.0f),
    new Vector3(5F, 10F, 5.0f),
    new Vector3(-5F, 10F, 5.0f),
    new Vector3(-5F, 0F, -5.0f),
    new Vector3(5F, 0F, -5.0f),
    new Vector3(5F, 10F, -5.0f),
    new Vector3(-5F, 10F, -5.0f)
};
        
int[] indices = new int[]
{
        0,1,2, 0, 2, 3, // Front face (Z+)
        1,5,6, 1, 6, 2, // Right side (X+)
        5,4,7, 5, 7, 6, // Back face (Z-)
        4,0,3, 4, 3, 7, // Left side (X-)
        0,4,5, 0, 5, 1, // Bottom face (Y-)
        3,2,6, 3, 6, 7 // Top face (Y+)
};
        
DracoMesh mesh = new DracoMesh();
PointAttribute attrPos = PointAttribute.wrap(AttributeType.POSITION, controlPoints);
mesh.addAttribute(attrPos);
//add triangle indices
mesh.getIndices().addRange(indices);
//number of the control points, it's required for the encoder to produce correct result.
mesh.setNumPoints(8);
DracoEncodeOptions opt = new DracoEncodeOptions();
byte[] drcBytes = Draco.encode(mesh, opt);
```

#### Convert Draco to OBJ format:
```
byte[] cube = Files.readAllBytes(Paths.get("TestData/cube.drc"));
DracoMesh dm = (DracoMesh)Draco.decode(cube);
PointAttribute attrPos = dm.getNamedAttribute(AttributeType.POSITION);
FloatSpan points = attrPos.getBuffer().asSpan().asFloatSpan();
StringBuilder sb = new StringBuilder();
for (int i = 0; i < points.size(); i += 3)
{
    sb.append(String.format("v %f %f %f\n", points.get(i), points.get(i + 1), points.get(i + 2)));
}

int[] face = new int[3];
for (int i = 0; i < dm.getNumFaces(); i++)
{
    dm.readFace(i, face);
    int a = attrPos.mappedIndex(face[0]) + 1;
    int b = attrPos.mappedIndex(face[1]) + 1;
    int c = attrPos.mappedIndex(face[2]) + 1;
    sb.append(String.format("f %d %d %d\n", a, b, c));
}

Files.writeString(Paths.get("output.obj"), sb.toString());
```


## License
FileFormat.Drako is available under [Openize License](LICENSE).
> [!CAUTION]
> FileFormat does not and cannot grant You a patent license for the utilization of [Google Draco](https://github.com/google/draco) compression/decompression technologies.

## OSS Notice
Sample files used for tests and located in the "TestsData" folder belong to [Google Draco](https://github.com/google/draco) and are used according to [Apache License 2.0](https://github.com/google/draco/blob/main/LICENSE)


## Coming updates
FileFormat.Drako will receive new features and regular updates to stay in sync with the latest versions of [Google Draco](https://github.com/google/draco). We appreciate your patience as we work on these improvements. Stay tuned for more updates soon.

