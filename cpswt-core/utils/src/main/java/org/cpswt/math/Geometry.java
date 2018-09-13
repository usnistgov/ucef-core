/*
 * Copyright (c) 2008, Institute for Software Integrated Systems, Vanderbilt University
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE VANDERBILT UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE VANDERBILT
 * UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE VANDERBILT UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE VANDERBILT UNIVERSITY HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * @author Gyorgy Balogh
 */

package org.cpswt.math;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;
import java.util.StringTokenizer;

import org.cpswt.utils.RandomSingleton;

//import Jama.Matrix;

public class Geometry
{
    public static final double ONE_DEGREE = Math.PI / 180;
    
    /*public static double[] calcEulerAngles( Vec3D x_axis, Vec3D y_axis )
    {
        double alfa, beta, gamma;
        
        Vec3D z_axis = x_axis.cross(y_axis);
        
        beta = Vec3D.calcAngle( z_axis, Vec3D.ZAXIS );
        
        beta = 0;
        alfa = 0;
        gamma = 0;
        
        System.out.println(z_axis);
        
        return new double[] {alfa, beta, gamma};                
    }*/
    
    public static Object[] fitLine( Vec3D p[], double min_dist )
    {
        int i,j;
        
        // avg pos
        Vec3D avg_pos = new Vec3D(0,0,0);       
        for( i=0; i<p.length; ++i )
            avg_pos = avg_pos.add(p[i]);
        avg_pos = avg_pos.scale(1/(double)p.length);        
        
        // avg dir
        Vec3D avg_dir = new Vec3D(0,0,0);
        int n = 0;        
        for( i=0; i<p.length-1; ++i )
        {
            for( j=i+1; j<p.length; ++j )
            {
                if( p[i].distance(p[j]) > min_dist )
                {
                    Vec3D d = p[i].sub(p[j]);
                    if( n==0 )
                        avg_dir.add(d);
                    else
                    {
                        if( Vec3D.calcAngle(avg_dir,d) < Vec3D.calcAngle(avg_dir,d.scale(-1)))
                            avg_dir = avg_dir.add(d);
                        else
                            avg_dir = avg_dir.add(d.scale(-1));
                    }
                    n++;                    
                }                
            }
        }
        avg_dir.norm();
        
        // calc error
        Vec3D p2 = avg_pos.add(avg_dir);
        double error = 0;
        for( i=0; i<p.length; ++i )
            error += pointLineDist( avg_pos, p2, p[i] );
        error /= (double)p.length;
        
        return new Object[] {avg_pos, avg_dir, new Double(error)};
    }
        
    public static boolean areFourPointsInOnePlane( Vec3D[] p )
    {
        Vec3D d1 = p[1].sub(p[0]).cross(p[1].sub(p[2]));
        Vec3D d2 = p[1].sub(p[0]).cross(p[1].sub(p[3]));        
        return( Vec3D.calcAngle(d1,d2) < 0.0001 );
    }
    
    /**
     * Compute position based on known positions of three anchors and distances from the anchors.
     * @param p1 First anchor
     * @param p2 Second anchor
     * @param p3 Third anchor
     * @param d1 distance form first anchor
     * @param d2 distance from second anchor
     * @param d3 distance from third anchor
     * @return The computed positions (there are two solutions) 
     */
    public static Vec3D[] localize( Vec3D p1, Vec3D p2, Vec3D p3, double d1, double d2, double d3 )
    {
        // set up a new coordinate system: p1 is origin (0,0,0), p2 is on x axis (x2,0,0), p3 is on x-y plane (x3,y3,0)
        double x2 = p1.distance(p2);
        double d_p1_p3 = p1.distance(p3);
        double d_p2_p3 = p2.distance(p3);
        double l = (d_p2_p3*d_p2_p3-d_p1_p3*d_p1_p3+x2*x2) / (2*x2);
        double x3 = x2 - l;
        double y3 = Math.sqrt(d_p2_p3*d_p2_p3-l*l);
               
        // calculate p4 position
        double x4 = (x2*x2 - d2*d2 + d1*d1) / (2*x2);
        double y4 = (x3*x3 - 2*x3*x4 + y3*y3 - d3*d3 + d1*d1) / (2*y3);
        double z4_squared = d1*d1 - x4*x4 - y4*y4;        
        double z4 = 0;        
        if( z4_squared>0 )
            z4 = Math.sqrt(z4_squared);
        
        // transform p4 back to original coordinate system
        Vec3D base_x = p2.sub(p1);
        base_x.norm();
        Vec3D base_z = base_x.cross(p3.sub(p1));
        base_z.norm();
        Vec3D base_y = base_z.cross(base_x);
        
        Vec3D res1 = p1.add(base_x.scale(x4).add(base_y.scale(y4)).add(base_z.scale(z4)));
        Vec3D res2 = p1.add(base_x.scale(x4).add(base_y.scale(y4)).add(base_z.scale(-z4)));
        
        return new Vec3D[] {res1, res2};
    }
    
    public static void testLocalize()
    {
        Random rand = RandomSingleton.instance();
        
        // generate random points in (0,0,0),(1,1,1) box
        Vec3D p[] = new Vec3D[4];
        for( int i=0; i<p.length; ++i )
        {
            p[i] = new Vec3D( rand.nextDouble(), rand.nextDouble(), rand.nextDouble() );
            System.out.println(p[i]);
        }
         
        Vec3D p3[] = localize( p[0], p[1], p[2], p[3].distance(p[0]), p[3].distance(p[1]), p[3].distance(p[2]));
        
        if( p3==null )
        {
            System.out.println("localize returned null");
        }
        else
        {        
            for( int i=0; i<p3.length; ++i )
                System.out.println( "dist=" + p3[i].distance(p[3]));
        }
    }
    
    /** line-line intersection
     * (based on http://mathworld.wolfram.com/Line-LineIntersection.html)
     * 
     * @param p1 first point on line one
     * @param p2 second point on line one
     * @param p3 first point on line two
     * @param p4 second point on line two
     * @return the intersection point or null if there is no intersection
     */
    public static Vec3D lineLineIntersection( Vec3D p1, Vec3D p2, Vec3D p3, Vec3D p4 )
    {
        Vec3D a = p2.sub(p1); 
        Vec3D b = p4.sub(p3);
        Vec3D c = p3.sub(p1);
        
        double d = Math.pow(a.cross(b).length(),2);
        
        if( d==0 )
            return null;
        
        double d2 = c.cross(b).dot(a.cross(b))/d;
        
        return p1.add(a.scale(d2));
    }
    
    /**
     * Calculates the closest points of two lines
     * @param p one point on the first line
     * @param u direction of the first line
     * @param q one point on the second line
     * @param v direction of the second line
     * @return two element array: the two closest points
     */
    public static Vec3D[] closestPointsOfTwoLines( Vec3D p, Vec3D u, Vec3D q, Vec3D v )
    {
        Vec3D w = p.sub(q);

        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a*c - b*b;        
        double sc, tc;

        // compute the line parameters of the two closest points
        if (D < 0.0001 ) 
        {         
            // the lines are almost parallel
            sc = 0.0;
            tc = (b>c ? d/b : e/c);   // use the largest denominator
        }
        else 
        {
            sc = (b*e - c*d) / D;
            tc = (a*e - b*d) / D;
        }
        
        Vec3D p1 = p.add(u.scale(sc));
        Vec3D q1 = q.add(v.scale(tc));
        
        return new Vec3D[]{p1,q1};
    }
    
    /**
     * Checks if a half line intersects a segment 
     * @param p1 start point of the half line
     * @param p2 other point on the half line
     * @param p3 segment endpoint
     * @param p4 segment endpoint
     * @return true if the half line intersects the segment
     */
    public static boolean doesHalfLineIntersectSegment( Vec3D p1, Vec3D p2, Vec3D p3, Vec3D p4 )
    {
        Vec3D a = p2.sub(p1); 
        Vec3D b = p4.sub(p3);
        Vec3D c = p3.sub(p1);
        
        double d = Math.pow(a.cross(b).length(),2);
        
        if( d==0 )
            return false;
        
        double d2 = c.cross(b).dot(a.cross(b))/d;
        
        Vec3D i = p1.add(a.scale(d2));
        
        // check if i on p1's right part
        if( d2<0 )
            return false;
                
        // check whether i is between p3 and p4
        return( Math.abs(p3.distance(i) + p4.distance(i) - p3.distance(p4))<0.00001 );        
    }
    
    /*public static boolean doesHalfLineIntersectXYRectangle( Vec3D min, Vec3D max, Vec3D p0, Vec3D p1 )  
    {
        double s = linePlaneIntersectionCoeff( min, max, new Vec3D( min.x, max.y, min.z ), p0, p1 );
        
        if( s<0 )
            return false;
        
        Vec3D p = p0.add( p1.sub(p0).scale(s) );
        
        return ( p.x>=min.x && p.x<=max.x && p.y>=min.y && p.y<=max.y );
    }
    
    public static boolean doesHalfLineIntersectXZRectangle( Vec3D min, Vec3D max, Vec3D p0, Vec3D p1 )  
    {
        double s = linePlaneIntersectionCoeff( min, max, new Vec3D( min.x, min.y, max.z ), p0, p1 );
        
        if( s<0 )
            return false;
        
        Vec3D p = p0.add( p1.sub(p0).scale(s) );
        
        return ( p.x>=min.x && p.x<=max.x && p.z>=min.z && p.z<=max.z );
    }
    
    public static boolean doesHalfLineIntersectYZRectangle( Vec3D min, Vec3D max, Vec3D p0, Vec3D p1 )  
    {
        double s = linePlaneIntersectionCoeff( min, max, new Vec3D( min.x, min.y, max.z ), p0, p1 );
        
        if( s<0 )
            return false;
        
        Vec3D p = p0.add( p1.sub(p0).scale(s) );
        
        return ( p.y>=min.y && p.y<=max.y && p.z>=min.z && p.z<=max.z );
    }*/
    
    /**
     * Calculates the closest point of a line to a given point
     * @param p1 one point on the line
     * @param p2 another point on the line
     * @param p3 the point
     * @return closest point of line to the point
     */
    public static Vec3D closestPointOfLineToPoint( Vec3D l0, Vec3D l1, Vec3D p )
    {
        Vec3D v = l1.sub(l0);
        Vec3D w = p.sub(l0);

        double c1 = v.dot(w);
        double c2 = v.dot(v);
        double b = c1 / c2;
        
        Vec3D pl = l0.add(v.scale(b));
        
        return pl;
    }
    
    public static double pointLineDist( Vec3D l0, Vec3D l1, Vec3D p )
    {
        return closestPointOfLineToPoint(l0,l1,p).distance(p);
    }
    
    /**
     * Calculates the intersection of a line and a plane. 
     * @param p1 first point on plane.
     * @param p2 second point on plane.
     * @param p3 third point on plane.
     * @param p4 first point on line.
     * @param p5 second point on line.
     * @return intersection point or null if the plane and line is paralell. 
     */
    /*public static Vec3D linePlaneIntersection( Vec3D p1, Vec3D p2, Vec3D p3, Vec3D p4, Vec3D p5 )
    {
        double scale = linePlaneIntersectionCoeff( p1, p2, p3, p4, p5 );
        
        if( scale == Double.MAX_VALUE )
            return null;
        
        return p4.add( p5.sub(p4).scale(scale) );        
    }*/
    
    /*public static double linePlaneIntersectionCoeff( Vec3D p1, Vec3D p2, Vec3D p3, Vec3D p4, Vec3D p5 )
    {        
        double m1[][] = new double[4][4];        
        
        m1[0][0] = 1;
        m1[0][1] = 1;
        m1[0][2] = 1;
        m1[0][3] = 1;
        
        m1[1][0] = p1.x;
        m1[1][1] = p2.x;
        m1[1][2] = p3.x;
        m1[1][3] = p4.x;
        
        m1[2][0] = p1.y;
        m1[2][1] = p2.y;
        m1[2][2] = p3.y;
        m1[2][3] = p4.y;
        
        m1[3][0] = p1.z;
        m1[3][1] = p2.z;
        m1[3][2] = p3.z;
        m1[3][3] = p4.z;
        
        double m2[][] = new double[4][4];
        
        m2[0][0] = 1;
        m2[0][1] = 1;
        m2[0][2] = 1;
        m2[0][3] = 0;
        
        m2[1][0] = p1.x;
        m2[1][1] = p2.x;
        m2[1][2] = p3.x;
        m2[1][3] = p5.x - p4.x;
        
        m2[2][0] = p1.y;
        m2[2][1] = p2.y;
        m2[2][2] = p3.y;
        m2[2][3] = p5.y - p4.y;
        
        m2[3][0] = p1.z;
        m2[3][1] = p2.z;
        m2[3][2] = p3.z;
        m2[3][3] = p5.z - p4.z;
        
        Matrix m1m = new Matrix( m1 );
        Matrix m2m = new Matrix( m2 );
        
        double m1_det = m1m.det();
        double m2_det = m2m.det();
        
        if( m2_det == 0 )
            return Double.MAX_VALUE;
        
        return -m1_det/m2_det;      
    }*/
    
    /**
     * Normalize an angle to 0..2pi
     * @param a angle to normalize
     * @return normalized angle
     */
    public static double normAngle( double a )
    {        
        double a1 = Math.atan2( Math.sin(a), Math.cos(a) );        
        if( a1<0 )
            a1 += 2*Math.PI;
        return a1;
    }
    
    public static double angleDiff( double angle1, double angle2 )
    {
        double a1 = normAngle(angle1);
        double a2 = normAngle(angle2);
        
        double d = a2 - a1;
        
        if( d > Math.PI )
        {
            d  -= 2*Math.PI;
        }
        else if( d < -Math.PI )
        {
            d  += 2*Math.PI;
        }
        
        return d;
    }
    
    /**
     * Calculates the absoulte difference of two angles.
     * @param angle1
     * @param angle2
     * @return difference of two angles
     */
    public static double absAngleDiff( double angle1, double angle2 )
    {
        double a1 = normAngle(angle1);
        double a2 = normAngle(angle2);
        
        double max,min;
        if( a1>a2 )
        {
            max = a1;
            min = a2;
        }
        else
        {
            max = a2;
            min = a1;
        }
        
        double diff1 = max - min;
        double diff2 = 2*Math.PI - diff1;
        
        if( diff1<diff2 )
            return diff1;
        else
            return diff2;       
    }    
    
    public static void main(String[] args) throws Exception
    {
        Vec3D t6 = new Vec3D(34.357056,-38.1896112,0);
        Vec3D t10 = new Vec3D(57.079896,-17.8442112,0);
        
        BufferedReader in = new BufferedReader(new FileReader("c:/temp/traj.txt"));
        String line = in.readLine();
        while( line != null )
        {
            StringTokenizer tokenizer = new StringTokenizer(line);
            if( tokenizer.hasMoreTokens() )
            {
                String p0x_str = tokenizer.nextToken();
                if( p0x_str.compareTo("na") != 0 )
                {
                    double p0x = Double.parseDouble(p0x_str);
                    double p0y = Double.parseDouble(tokenizer.nextToken());
                    double p0z = Double.parseDouble(tokenizer.nextToken());
                    double dx = Double.parseDouble(tokenizer.nextToken());
                    double dy = Double.parseDouble(tokenizer.nextToken());
                    double dz = Double.parseDouble(tokenizer.nextToken());
                    
                    Vec3D p0  = new Vec3D(p0x,p0y,0);
                    Vec3D dir = new Vec3D(dx,dy,0);
                    Vec3D p1  = p0.add(dir);
                    
                    Vec3D i = lineLineIntersection( t6, t10, p0, p1 );
                    System.out.println(i);                    
                }
                else
                {
                    System.out.println("na\tna\tna\t");
                }
            }
            line = in.readLine();
        }
        
        


    }
}
